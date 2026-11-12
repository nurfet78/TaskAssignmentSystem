package org.nurfet.taskassignmentsystem;

import org.junit.jupiter.api.RepeatedTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты на гонки: несколько запросов стартуют одновременно.
 * Каждый тест повторяется, потому что гонка проявляется не при каждом запуске.
 * Очистка БД выполняется перед каждым повтором (см. AbstractApiTest).

 * Чтобы убедиться, что тесты чувствительны к ошибкам, можно временно убрать защиту
 * (например, заменить findByIdWithLock на findById в DeveloperService.update)
 * и посмотреть, что тест assignAndDeactivate начнёт падать.
 */
class RaceConditionIntegrationTest extends AbstractApiTest {

    private static final int THREADS = 8;

    /**
     * Любой 409 в гонках по email должен быть именно дубликатом (/error/duplicate).
     * Общая ошибка целостности (/error/data_integrity) значит, что перехват нарушения
     * уникального индекса (isEmailConstraintViolation) не сработал: статус тоже 409,
     * поэтому одной проверки кода недостаточно.
     */
    private static void assertAllConflictsAreDuplicates(List<ApiResult> results) {
        List<ApiResult> conflicts = results.stream().filter(r -> r.status() == 409).toList();

        assertTrue(conflicts.stream().allMatch(r -> r.type() != null && r.type().endsWith("/duplicate")),
                "409 должен быть дубликатом, а не общей ошибкой целостности: " + conflicts);
    }

    // =====================================================================
    // Уникальность
    // =====================================================================

    @RepeatedTest(5)
    void concurrentCreateSkillWithSameName_onlyOneSucceeds() throws Exception {
        String[] variants = {"Java", "java", "JAVA", " Java ", "jAvA", "JaVa", "java ", " JAVA"};
        List<Callable<Integer>> calls = new ArrayList<>();
        for (String name : variants) {
            calls.add(() -> status(post(SKILLS, Map.of("name", name))));
        }

        List<Integer> statuses = runConcurrently(calls);

        assertEquals(1, count(statuses, 201), "статусы: " + statuses);
        assertEquals(variants.length - 1, count(statuses, 409), "статусы: " + statuses);
        assertEquals(1, jdbc.queryForObject("select count(*) from skills", Integer.class));
    }

    @RepeatedTest(5)
    void concurrentCreateDeveloperWithSameEmail_onlyOneSucceeds() throws Exception {
        String[] variants = {"john@example.com", "JOHN@example.com", "John@Example.Com", "john@EXAMPLE.com"};
        List<Callable<ApiResult>> calls = new ArrayList<>();
        for (String email : variants) {
            calls.add(() -> result(post(DEVELOPERS,
                    Map.of("firstName", "Ivan", "lastName", "Petrov", "email", email))));
        }

        List<ApiResult> results = runConcurrently(calls);
        List<Integer> statuses = results.stream().map(ApiResult::status).toList();

        assertEquals(1, count(statuses, 201), "статусы: " + statuses);
        assertEquals(variants.length - 1, count(statuses, 409), "статусы: " + statuses);
        assertAllConflictsAreDuplicates(results);
        assertEquals(1, jdbc.queryForObject("select count(*) from developers", Integer.class));
    }

    @RepeatedTest(5)
    void concurrentChangeEmailToSameAddress_onlyOneSucceeds() throws Exception {
        long first = createDeveloper("first@example.com");
        long second = createDeveloper("second@example.com");
        long firstVersion = developerVersion(first);
        long secondVersion = developerVersion(second);

        List<ApiResult> results = runConcurrently(List.of(
                () -> result(patch(DEVELOPERS + "/{id}/email",
                        Map.of("version", firstVersion, "newEmail", "shared@example.com"), first)),
                () -> result(patch(DEVELOPERS + "/{id}/email",
                        Map.of("version", secondVersion, "newEmail", "SHARED@example.com"), second))));
        List<Integer> statuses = results.stream().map(ApiResult::status).toList();

        assertEquals(1, count(statuses, 200), "статусы: " + statuses);
        assertEquals(1, count(statuses, 409), "статусы: " + statuses);
        assertAllConflictsAreDuplicates(results);
    }

    @RepeatedTest(5)
    void concurrentRenameSkillsToSameName_onlyOneSucceeds() throws Exception {
        long a = createSkill("A");
        long b = createSkill("B");

        List<Integer> statuses = runConcurrently(List.of(
                () -> status(patch(SKILLS + "/{id}", Map.of("name", "Shared"), a)),
                () -> status(patch(SKILLS + "/{id}", Map.of("name", "SHARED"), b))));

        assertEquals(1, count(statuses, 200), "статусы: " + statuses);
        assertEquals(1, count(statuses, 409), "статусы: " + statuses);
    }

    // =====================================================================
    // Блокировки и версии
    // =====================================================================

    /**
     * Главный инвариант: у неактивного разработчика не должно быть задач в работе.
     * Деактивация и назначение стартуют одновременно: успешной должна быть ровно одна операция.
     */
    @RepeatedTest(20)
    void concurrentAssignAndDeactivate_neverLeavesInactiveDeveloperWithTask() throws Exception {
        long java = createSkill("Java");
        long dev = createDeveloper("dev@example.com", java);
        long task = createTask("Task", java);
        long devVersion = developerVersion(dev);
        long taskVersion = taskVersion(task);

        List<Integer> statuses = runConcurrently(List.of(
                () -> status(patch(DEVELOPERS + "/{id}",
                        Map.of("version", devVersion, "active", false), dev)),
                () -> status(post(TASKS + "/{id}/assign",
                        Map.of("developerId", dev, "version", taskVersion), task))));

        boolean active = jdbc.queryForObject("select active from developers where id = ?", Boolean.class, dev);
        int inProgress = jdbc.queryForObject(
                "select count(*) from tasks where assignee_id = ? and status = 'IN_PROGRESS'",
                Integer.class, dev);

        assertFalse(!active && inProgress > 0,
                "неактивный разработчик с задачей в работе, статусы: " + statuses);
        assertEquals(1, countSuccessful(statuses), "должна пройти ровно одна операция: " + statuses);
    }

    @RepeatedTest(10)
    void concurrentAssignOfSameTask_onlyOneSucceeds() throws Exception {
        long java = createSkill("Java");
        long first = createDeveloper("first@example.com", java);
        long second = createDeveloper("second@example.com", java);
        long task = createTask("Task", java);
        long version = taskVersion(task);

        List<Integer> statuses = runConcurrently(List.of(
                () -> status(post(TASKS + "/{id}/assign", Map.of("developerId", first, "version", version), task)),
                () -> status(post(TASKS + "/{id}/assign", Map.of("developerId", second, "version", version), task))));

        // если здесь 500, в обработчике нет маппинга ObjectOptimisticLockingFailureException на 409
        assertEquals(1, count(statuses, 200), "статусы: " + statuses);
        assertEquals(1, count(statuses, 409), "статусы: " + statuses);
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from tasks where id = ? and assignee_id is not null", Integer.class, task));
    }

    @RepeatedTest(10)
    void concurrentAutoAssignOfSameTask_assignsOnlyOnce() throws Exception {
        long java = createSkill("Java");
        createDeveloper("first@example.com", java);
        createDeveloper("second@example.com", java);
        long task = createTask("Task", java);

        List<Callable<Integer>> calls = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            calls.add(() -> status(postEmpty(TASKS + "/{id}/auto-assign", task)));
        }

        List<Integer> statuses = runConcurrently(calls);

        assertEquals(1, countSuccessful(statuses), "статусы: " + statuses);
        assertTrue(statuses.stream().noneMatch(s -> s >= 500), "серверные ошибки: " + statuses);
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from task_status_events where task_id = ? and new_status = 'IN_PROGRESS'",
                Integer.class, task));
    }

    /**
     * Удаление навыка и создание разработчика с этим навыком стартуют одновременно.
     * Допустимо одно из двух: навык удалён, а разработчик не создан, либо разработчик создан,
     * а навык остался. Нельзя, чтобы прошли обе операции.
     */
    @RepeatedTest(15)
    void concurrentDeleteSkillAndAttachToDeveloper_notBothSucceed() throws Exception {
        long skill = createSkill("Java");

        List<Integer> statuses = runConcurrently(List.of(
                () -> status(delete(SKILLS + "/{id}", skill)),
                () -> status(post(DEVELOPERS, Map.of(
                        "firstName", "Ivan",
                        "lastName", "Petrov",
                        "email", "dev@example.com",
                        "skillIds", List.of(skill))))));

        int skills = jdbc.queryForObject("select count(*) from skills", Integer.class);
        int links = jdbc.queryForObject("select count(*) from developer_skills", Integer.class);

        assertFalse(statuses.get(0) == 204 && statuses.get(1) == 201,
                "прошли обе операции, статусы: " + statuses);
        assertTrue(links == 0 || skills == 1, "связь указывает на удалённый навык");
        assertTrue(statuses.stream().noneMatch(s -> s >= 500), "серверные ошибки: " + statuses);
    }
}
