package org.nurfet.taskassignmentsystem;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReportApiIntegrationTest extends AbstractApiTest {

    // ---------- Хелперы ----------

    private long createTaskWithPriority(String title, String priority, Long... skillIds) {
        Map<String, Object> body = Map.of(
                "title", title,
                "priority", priority,
                "skillIds", List.of(skillIds));
        return number(bodyOf(post(TASKS, body).expectStatus().isCreated()), "id");
    }

    private void changeStatus(long taskId, String status) {
        patch(TASKS + "/{id}/status",
                Map.of("newStatus", status, "version", taskVersion(taskId)), taskId)
                .expectStatus().isOk();
    }

    /** Создаёт задачу, назначает разработчику и переводит в DONE. */
    private long doneTask(String title, String priority, long skillId, long developerId) {
        long task = createTaskWithPriority(title, priority, skillId);
        assign(task, developerId).expectStatus().isOk();
        changeStatus(task, "DONE");
        return task;
    }

    private void setTaskTimes(long taskId, Instant createdAt, Instant assignedAt, Instant completedAt) {
        jdbc.update("update tasks set created_at = ?, assigned_at = ?, completed_at = ? where id = ?",
                Timestamp.from(createdAt), Timestamp.from(assignedAt), Timestamp.from(completedAt), taskId);
    }

    private static void assertNumber(double expected, Number actual) {
        assertEquals(expected, actual.doubleValue(), 0.001);
    }

    // =====================================================================
    // Шаг 1. Статистика задач
    // =====================================================================

    @Test
    void statistics_emptyDatabase_returnsZerosWithoutDivisionByZero() {
        get(REPORTS + "/tasks/statistics")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalTasks").isEqualTo(0)
                .jsonPath("$.byStatus.length()").isEqualTo(4)
                .jsonPath("$.byStatus[0].percent").value(Number.class, n -> assertNumber(0, n))
                .jsonPath("$.completionByPriority.length()").isEqualTo(4)
                .jsonPath("$.completionByPriority[0].completedTasks").isEqualTo(0)
                .jsonPath("$.completionByPriority[0].avgHours").isEmpty();
    }

    @Test
    void statistics_distribution_coversAllFourStatuses() {
        long java = createSkill("Java");
        long dev = createDeveloper("dev@example.com", java);
        createTask("New 1");
        createTask("New 2");
        assign(createTask("In progress", java), dev).expectStatus().isOk();
        doneTask("Done", "LOW", java, dev);
        changeStatus(createTask("Cancelled"), "CANCELLED");

        // порядок статусов как в enum: NEW, IN_PROGRESS, DONE, CANCELLED.
        // Тест заодно проверяет литералы статусов в SQL: у каждого статуса ненулевой счётчик.
        get(REPORTS + "/tasks/statistics")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalTasks").isEqualTo(5)
                .jsonPath("$.byStatus[0].status").isEqualTo("NEW")
                .jsonPath("$.byStatus[0].count").isEqualTo(2)
                .jsonPath("$.byStatus[0].percent").value(Number.class, n -> assertNumber(40.0, n))
                .jsonPath("$.byStatus[1].status").isEqualTo("IN_PROGRESS")
                .jsonPath("$.byStatus[1].count").isEqualTo(1)
                .jsonPath("$.byStatus[1].percent").value(Number.class, n -> assertNumber(20.0, n))
                .jsonPath("$.byStatus[2].status").isEqualTo("DONE")
                .jsonPath("$.byStatus[2].count").isEqualTo(1)
                .jsonPath("$.byStatus[3].status").isEqualTo("CANCELLED")
                .jsonPath("$.byStatus[3].count").isEqualTo(1);
    }

    @Test
    void statistics_completionTime_usesOnlyDoneTasksInImportanceOrder() {
        long java = createSkill("Java");
        long dev = createDeveloper("dev@example.com", java);
        Instant base = Instant.parse("2026-01-01T00:00:00Z");

        long h1 = doneTask("H1", "HIGH", java, dev);
        long h2 = doneTask("H2", "HIGH", java, dev);
        setTaskTimes(h1, base, base, base.plus(2, ChronoUnit.HOURS));
        setTaskTimes(h2, base, base, base.plus(4, ChronoUnit.HOURS));

        // completedAt заполняется и для CANCELLED, но в статистику она попасть не должна
        long cancelled = createTaskWithPriority("Cancelled", "CRITICAL", java);
        assign(cancelled, dev).expectStatus().isOk();
        changeStatus(cancelled, "CANCELLED");

        // порядок: CRITICAL, HIGH, MEDIUM, LOW
        get(REPORTS + "/tasks/statistics")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.completionByPriority[0].priority").isEqualTo("CRITICAL")
                .jsonPath("$.completionByPriority[0].completedTasks").isEqualTo(0)
                .jsonPath("$.completionByPriority[0].avgHours").isEmpty()
                .jsonPath("$.completionByPriority[1].priority").isEqualTo("HIGH")
                .jsonPath("$.completionByPriority[1].completedTasks").isEqualTo(2)
                .jsonPath("$.completionByPriority[1].avgHours").value(Number.class, n -> assertNumber(3.0, n))
                .jsonPath("$.completionByPriority[1].minHours").value(Number.class, n -> assertNumber(2.0, n))
                .jsonPath("$.completionByPriority[1].maxHours").value(Number.class, n -> assertNumber(4.0, n))
                .jsonPath("$.completionByPriority[2].priority").isEqualTo("MEDIUM")
                .jsonPath("$.completionByPriority[3].priority").isEqualTo("LOW");
    }

    /////////////////////////////////////////

    @Test
    void unassignable_listsOnlyTasksWithoutSuitableDeveloper_byPriority() {
        long java = createSkill("Java");
        long spring = createSkill("Spring");
        long go = createSkill("Go");
        createDeveloper("active@example.com", java);
        long inactive = createDeveloper("inactive@example.com", spring);
        setActive(inactive, false).expectStatus().isOk();

        long needsGo = createTaskWithPriority("Needs Go", "CRITICAL", go);          // никто не владеет
        long onlyInactive = createTaskWithPriority("Needs Spring", "HIGH", spring); // владеет только неактивный
        createTaskWithPriority("Partial", "MEDIUM", java, spring);                  // частичное совпадение есть
        createTask("No skills");                                                    // без навыков
        changeStatus(createTaskWithPriority("Cancelled", "LOW", go), "CANCELLED");  // не NEW

        get(REPORTS + "/tasks/unassignable")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].taskId").isEqualTo(needsGo)
                .jsonPath("$[0].requiredSkillsCount").isEqualTo(1)
                .jsonPath("$[1].taskId").isEqualTo(onlyInactive);
    }

    @Test
    void unassignable_agreesWithAutoAssign() {
        long java = createSkill("Java");
        long spring = createSkill("Spring");
        long go = createSkill("Go");
        createDeveloper("dev@example.com", java);

        long assignable = createTask("Partial match", java, spring);
        long unassignable = createTask("No match", go);

        get(REPORTS + "/tasks/unassignable")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].taskId").isEqualTo(unassignable);

        postEmpty(TASKS + "/{id}/auto-assign", unassignable).expectStatus().isEqualTo(409);
        postEmpty(TASKS + "/{id}/auto-assign", assignable).expectStatus().isOk();
    }

    @Test
    void unassignable_emptyDatabase_returnsEmptyList() {
        get(REPORTS + "/tasks/unassignable")
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void statistics_byPriority_emptyDatabase_hasAllFourWithZeros() {
        get(REPORTS + "/tasks/statistics")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.byPriority.length()").isEqualTo(4)
                .jsonPath("$.byPriority[0].priority").isEqualTo("CRITICAL")
                .jsonPath("$.byPriority[1].priority").isEqualTo("HIGH")
                .jsonPath("$.byPriority[2].priority").isEqualTo("MEDIUM")
                .jsonPath("$.byPriority[3].priority").isEqualTo("LOW")
                .jsonPath("$.byPriority[0].count").isEqualTo(0)
                .jsonPath("$.byPriority[0].percent").value(Number.class, n -> assertNumber(0, n));
    }

    @Test
    void statistics_byPriority_countsAllStatuses() {
        createTaskWithPriority("C", "CRITICAL");
        createTaskWithPriority("H", "HIGH");
        createTask("M1");                                   // MEDIUM по умолчанию
        long m2 = createTask("M2");
        changeStatus(m2, "CANCELLED");                      // отменённые тоже считаются

        get(REPORTS + "/tasks/statistics")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.byPriority[0].count").isEqualTo(1)
                .jsonPath("$.byPriority[0].percent").value(Number.class, n -> assertNumber(25.0, n))
                .jsonPath("$.byPriority[1].count").isEqualTo(1)
                .jsonPath("$.byPriority[2].count").isEqualTo(2)
                .jsonPath("$.byPriority[2].percent").value(Number.class, n -> assertNumber(50.0, n))
                .jsonPath("$.byPriority[3].count").isEqualTo(0);
    }
}
