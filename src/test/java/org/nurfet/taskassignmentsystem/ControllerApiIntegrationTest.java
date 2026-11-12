package org.nurfet.taskassignmentsystem;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HTTP-тесты контроллеров: коды ответов и маппинг исключений.
 * Ожидаемые коды: 404, 409 (конфликты и недопустимые состояния), 400 (валидация).
 * Для ошибок назначения и смены статуса код может быть 409 или 422
 */
class ControllerApiIntegrationTest extends AbstractApiTest {

    // =====================================================================
    // Навыки
    // =====================================================================

    @Test
    void skill_create_returns201() {
        post(SKILLS, Map.of("name", "Java", "description", "Язык"))
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.name").isEqualTo("Java");
    }

    @Test
    void skill_create_duplicateIgnoringCase_returns409() {
        createSkill("Java");

        post(SKILLS, Map.of("name", " jAVA ")).expectStatus().isEqualTo(409);
    }

    @Test
    void skill_create_blankName_returns400() {
        post(SKILLS, Map.of("name", "   ")).expectStatus().isBadRequest();
    }

    @Test
    void skill_create_tooLongName_returns400() {
        post(SKILLS, Map.of("name", "x".repeat(101))).expectStatus().isBadRequest();
    }

    @Test
    void skill_getById_notFound_returns404() {
        get(SKILLS + "/{id}", 999).expectStatus().isNotFound();
    }

    @Test
    void skill_getByName_ignoresCaseAndSpaces() {
        long id = createSkill("Java");

        get(SKILLS + "/by-name?name={name}", "  jAvA ")
                .expectStatus().isOk()
                .expectBody().jsonPath("$.id").isEqualTo(id);
    }

    @Test
    void skill_getByName_notFound_returns404() {
        get(SKILLS + "/by-name?name={name}", "Nope").expectStatus().isNotFound();
    }

    @Test
    void skill_getByName_blank_returns400() {
        get(SKILLS + "/by-name?name={name}", " ").expectStatus().isBadRequest();
    }

    @Test
    void skill_getByName_missingParam_returns400() {
        get(SKILLS + "/by-name").expectStatus().isBadRequest();
    }

    @Test
    void skill_update_returns200() {
        long id = createSkill("Java");

        patch(SKILLS + "/{id}", Map.of("description", "Новое описание"), id)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Java")
                .jsonPath("$.description").isEqualTo("Новое описание");
    }

    @Test
    void skill_update_toExistingName_returns409() {
        createSkill("Java");
        long spring = createSkill("Spring");

        patch(SKILLS + "/{id}", Map.of("name", "JAVA"), spring).expectStatus().isEqualTo(409);
    }

    @Test
    void skill_delete_whenUsed_returns409() {
        long java = createSkill("Java");
        createDeveloper("dev@example.com", java);

        delete(SKILLS + "/{id}", java).expectStatus().isEqualTo(409);
        get(SKILLS + "/{id}", java).expectStatus().isOk();
    }

    @Test
    void skill_delete_whenUnused_returns204() {
        long java = createSkill("Java");

        delete(SKILLS + "/{id}", java).expectStatus().isNoContent();
        get(SKILLS + "/{id}", java).expectStatus().isNotFound();
    }

    // =====================================================================
    // Разработчики
    // =====================================================================

    @Test
    void developer_create_returns201_andLowercasesEmail() {
        post(DEVELOPERS, Map.of("firstName", "Ivan", "lastName", "Petrov", "email", "John@Example.COM"))
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.email").isEqualTo("john@example.com");
    }

    @Test
    void developer_create_emailWithSurroundingSpaces_returns400() {
        // валидация @Email выполняется до нормализации в сервисе
        post(DEVELOPERS, Map.of("firstName", "Ivan", "lastName", "Petrov", "email", " john@example.com "))
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.fieldErrors[0].field").isEqualTo("email");
    }

    @Test
    void developer_create_duplicateEmail_returns409() {
        createDeveloper("john@example.com");

        post(DEVELOPERS, Map.of("firstName", "A", "lastName", "B", "email", "JOHN@example.com"))
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.type").isEqualTo("/error/duplicate");
    }

    @Test
    void developer_create_invalidEmail_returns400() {
        post(DEVELOPERS, Map.of("firstName", "A", "lastName", "B", "email", "not-an-email"))
                .expectStatus().isBadRequest();
    }

    @Test
    void developer_getById_notFound_returns404() {
        get(DEVELOPERS + "/{id}", 999).expectStatus().isNotFound();
    }

    @Test
    void developer_getByEmail_ignoresCase() {
        long id = createDeveloper("john@example.com");

        get(DEVELOPERS + "/by-email?email={email}", "John@Example.com")
                .expectStatus().isOk()
                .expectBody().jsonPath("$.id").isEqualTo(id);
    }

    @Test
    void developer_getByEmail_notFound_returns404() {
        get(DEVELOPERS + "/by-email?email={email}", "nobody@example.com").expectStatus().isNotFound();
    }

    @Test
    void developer_getByEmail_invalidEmail_returns400() {
        get(DEVELOPERS + "/by-email?email={email}", "not-an-email").expectStatus().isBadRequest();
    }

    @Test
    void developer_getAll_activeOnly_filtersInactive() {
        long active = createDeveloper("active@example.com");
        long inactive = createDeveloper("inactive@example.com");
        setActive(inactive, false).expectStatus().isOk();

        get(DEVELOPERS + "?activeOnly=true")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(1)
                .jsonPath("$.content[0].id").isEqualTo(active);
    }

    @Test
    void developer_deactivate_withActiveTask_returns409() {
        long java = createSkill("Java");
        long dev = createDeveloper("dev@example.com", java);
        assign(createTask("Task", java), dev).expectStatus().isOk();

        setActive(dev, false).expectStatus().isEqualTo(409);
    }

    @Test
    void developer_deactivate_withoutTasks_returns200() {
        long dev = createDeveloper("dev@example.com");

        setActive(dev, false)
                .expectStatus().isOk()
                .expectBody().jsonPath("$.active").isEqualTo(false);
    }

    @Test
    void developer_update_staleVersion_returns409() {
        long dev = createDeveloper("dev@example.com");
        long staleVersion = developerVersion(dev);
        patch(DEVELOPERS + "/{id}", Map.of("version", staleVersion, "firstName", "New", "active", true), dev)
                .expectStatus().isOk();

        patch(DEVELOPERS + "/{id}", Map.of("version", staleVersion, "firstName", "Other", "active", true), dev)
                .expectStatus().isEqualTo(409);
    }

    @Test
    void developer_changeEmail_returns200() {
        long dev = createDeveloper("old@example.com");

        patch(DEVELOPERS + "/{id}/email",
                Map.of("version", developerVersion(dev), "newEmail", "New@Example.com"), dev)
                .expectStatus().isOk()
                .expectBody().jsonPath("$.email").isEqualTo("new@example.com");
    }

    @Test
    void developer_changeEmail_withSurroundingSpaces_returns400() {
        long dev = createDeveloper("old@example.com");

        patch(DEVELOPERS + "/{id}/email",
                Map.of("version", developerVersion(dev), "newEmail", " new@example.com "), dev)
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.fieldErrors[0].field").isEqualTo("newEmail");
    }

    @Test
    void developer_changeEmail_toExisting_returns409() {
        createDeveloper("taken@example.com");
        long dev = createDeveloper("mine@example.com");

        patch(DEVELOPERS + "/{id}/email",
                Map.of("version", developerVersion(dev), "newEmail", "TAKEN@example.com"), dev)
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.type").isEqualTo("/error/duplicate");
    }

    @Test
    void developer_changeEmail_staleVersion_returns409() {
        long dev = createDeveloper("old@example.com");
        long staleVersion = developerVersion(dev);
        patch(DEVELOPERS + "/{id}/email", Map.of("version", staleVersion, "newEmail", "a@example.com"), dev)
                .expectStatus().isOk();

        patch(DEVELOPERS + "/{id}/email", Map.of("version", staleVersion, "newEmail", "b@example.com"), dev)
                .expectStatus().isEqualTo(409);
    }

    @Test
    void developer_taskCount_returnsNumber() {
        long java = createSkill("Java");
        long dev = createDeveloper("dev@example.com", java);
        assign(createTask("Task", java), dev).expectStatus().isOk();

        get(DEVELOPERS + "/{id}/task-count", dev)
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(1L);
    }

    // =====================================================================
    // Задачи
    // =====================================================================

    @Test
    void task_create_returns201_withStatusNew() {
        long java = createSkill("Java");

        post(TASKS, Map.of("title", "Task", "priority", "HIGH", "skillIds", List.of(java)))
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo("NEW")
                .jsonPath("$.priority").isEqualTo("HIGH");
    }

    @Test
    void task_create_blankTitle_returns400() {
        post(TASKS, Map.of("title", " ", "priority", "MEDIUM")).expectStatus().isBadRequest();
    }

    @Test
    void task_getById_notFound_returns404() {
        get(TASKS + "/{id}", 999).expectStatus().isNotFound();
    }

    @Test
    void task_getAll_filtersByStatus() {
        long java = createSkill("Java");
        long dev = createDeveloper("dev@example.com", java);
        createTask("Free", java);
        assign(createTask("Taken", java), dev).expectStatus().isOk();

        get(TASKS + "?status=NEW")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(1)
                .jsonPath("$.content[0].title").isEqualTo("Free");
    }

    @Test
    void task_getAll_invalidStatus_returns400() {
        get(TASKS + "?status=UNKNOWN").expectStatus().isBadRequest();
    }

    @Test
    void task_unassigned_returnsOnlyFreeTasks() {
        long java = createSkill("Java");
        long dev = createDeveloper("dev@example.com", java);
        createTask("Free", java);
        assign(createTask("Taken", java), dev).expectStatus().isOk();

        get(TASKS + "/unassigned")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].title").isEqualTo("Free");
    }

    @Test
    void task_update_returns200_andStaleVersionReturns409() {
        long task = createTask("Task");
        long version = taskVersion(task);

        patch(TASKS + "/{id}", Map.of("version", version, "title", "New title"), task)
                .expectStatus().isOk()
                .expectBody().jsonPath("$.title").isEqualTo("New title");

        patch(TASKS + "/{id}", Map.of("version", version, "title", "Other"), task)
                .expectStatus().isEqualTo(409);
    }

    @Test
    void task_assign_setsInProgress() {
        long java = createSkill("Java");
        long dev = createDeveloper("dev@example.com", java);
        long task = createTask("Task", java);

        assign(task, dev)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("IN_PROGRESS")
                .jsonPath("$.assignee.id").isEqualTo(dev);
    }

    @Test
    void task_assign_staleVersion_returns409() {
        long java = createSkill("Java");
        long dev = createDeveloper("dev@example.com", java);
        long task = createTask("Task", java);

        post(TASKS + "/{id}/assign", Map.of("developerId", dev, "version", taskVersion(task) + 5), task)
                .expectStatus().isEqualTo(409);
    }

    @Test
    void task_assign_toInactiveDeveloper_returns4xx() {
        long java = createSkill("Java");
        long dev = createDeveloper("dev@example.com", java);
        setActive(dev, false).expectStatus().isOk();
        long task = createTask("Task", java);

        assign(task, dev).expectStatus().is4xxClientError();
    }

    @Test
    void task_assign_developerWithoutSkill_returns4xx() {
        long java = createSkill("Java");
        long go = createSkill("Go");
        long dev = createDeveloper("dev@example.com", go);
        long task = createTask("Task", java);

        assign(task, dev).expectStatus().is4xxClientError();
    }

    @Test
    void task_assign_unknownDeveloper_returns404() {
        long task = createTask("Task");

        post(TASKS + "/{id}/assign", Map.of("developerId", 999, "version", taskVersion(task)), task)
                .expectStatus().isNotFound();
    }

    @Test
    void task_unassign_returnsTaskToNew() {
        long java = createSkill("Java");
        long dev = createDeveloper("dev@example.com", java);
        long task = createTask("Task", java);
        assign(task, dev).expectStatus().isOk();

        delete(TASKS + "/{id}/assignee?version={v}", task, taskVersion(task))
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("NEW")
                .jsonPath("$.assignee").isEmpty();
    }

    @Test
    void task_changeStatus_toInProgressWithoutAssignee_returns4xx() {
        long task = createTask("Task");

        patch(TASKS + "/{id}/status", Map.of("newStatus", "IN_PROGRESS", "version", taskVersion(task)), task)
                .expectStatus().is4xxClientError();
    }

    @Test
    void task_changeStatus_invalidTransition_returns4xx() {
        long task = createTask("Task");

        patch(TASKS + "/{id}/status", Map.of("newStatus", "DONE", "version", taskVersion(task)), task)
                .expectStatus().is4xxClientError();
    }

    @Test
    void task_changeStatus_toDone_returns200() {
        long java = createSkill("Java");
        long dev = createDeveloper("dev@example.com", java);
        long task = createTask("Task", java);
        assign(task, dev).expectStatus().isOk();

        patch(TASKS + "/{id}/status", Map.of("newStatus", "DONE", "version", taskVersion(task)), task)
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("DONE");
    }

    @Test
    void task_autoAssign_picksActiveDeveloper() {
        long java = createSkill("Java");
        long inactive = createDeveloper("inactive@example.com", java);
        long active = createDeveloper("active@example.com", java);
        setActive(inactive, false).expectStatus().isOk();
        long task = createTask("Task", java);

        postEmpty(TASKS + "/{id}/auto-assign", task).expectStatus().isOk();

        get(TASKS + "/{id}", task)
                .expectStatus().isOk()
                .expectBody().jsonPath("$.assignee.id").isEqualTo(active);
    }

    @Test
    void task_autoAssign_noCandidates_returns4xx() {
        long java = createSkill("Java");
        long task = createTask("Task", java);

        postEmpty(TASKS + "/{id}/auto-assign", task).expectStatus().is4xxClientError();
    }

    @Test
    void task_delete_inProgress_returns409() {
        long java = createSkill("Java");
        long dev = createDeveloper("dev@example.com", java);
        long task = createTask("Task", java);
        assign(task, dev).expectStatus().isOk();

        delete(TASKS + "/{id}", task).expectStatus().isEqualTo(409);
    }

    @Test
    void task_delete_new_returns204() {
        long task = createTask("Task");

        delete(TASKS + "/{id}", task).expectStatus().isNoContent();
        get(TASKS + "/{id}", task).expectStatus().isNotFound();
    }

    @Test
    void task_history_containsCreationAndAssignment() {
        long java = createSkill("Java");
        long dev = createDeveloper("dev@example.com", java);
        long task = createTask("Task", java);
        assign(task, dev).expectStatus().isOk();

        get(TASKS + "/{id}/history", task)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").value(Integer.class, n -> assertTrue(n >= 2));
    }

    @Test
    void task_history_unknownTask_returns404() {
        get(TASKS + "/{id}/history", 999).expectStatus().isNotFound();
    }

    @Test
    void unknownEndpoint_doesNotLeakAs500() {
        int code = status(get("/api/v1/nope"));

        assertEquals(404, code);
    }
}
