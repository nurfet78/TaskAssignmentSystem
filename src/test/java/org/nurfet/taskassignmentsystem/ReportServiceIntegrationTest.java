package org.nurfet.taskassignmentsystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nurfet.taskassignmentsystem.domain.enums.TaskPriority;
import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;
import org.nurfet.taskassignmentsystem.dto.request.*;
import org.nurfet.taskassignmentsystem.dto.response.DeveloperResponse;
import org.nurfet.taskassignmentsystem.dto.response.SkillResponse;
import org.nurfet.taskassignmentsystem.dto.response.StatusDurationResponse;
import org.nurfet.taskassignmentsystem.dto.response.TaskResponse;
import org.nurfet.taskassignmentsystem.exception.ResourceNotFoundException;
import org.nurfet.taskassignmentsystem.service.DeveloperService;
import org.nurfet.taskassignmentsystem.service.ReportService;
import org.nurfet.taskassignmentsystem.service.SkillService;
import org.nurfet.taskassignmentsystem.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ReportServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    SkillService skillService;
    @Autowired
    DeveloperService developerService;
    @Autowired
    TaskService taskService;
    @Autowired
    ReportService reportService;
    @Autowired
    JdbcTemplate jdbc;

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("""
                truncate task_status_events, task_skills, tasks,
                         developer_skills, developers, skills
                restart identity cascade
                """);
    }

    // ---------- Хелперы ----------

    private SkillResponse createSkill(String name) {
        return skillService.create(new CreateSkillRequest(name, null));
    }

    private DeveloperResponse createDeveloper(String email, Long... skillIds) {
        return developerService.create(
                new CreateDeveloperRequest("Ivan", "Petrov", email, Set.of(skillIds)));
    }

    private TaskResponse createTask(String title, Long... skillIds) {
        return taskService.create(
                new CreateTaskRequest(title, null, TaskPriority.MEDIUM, Set.of(skillIds)));
    }

    private TaskResponse assign(TaskResponse task, DeveloperResponse developer) {
        return taskService.assignToDeveloper(task.id(),
                new AssignTaskRequest(task.version(), developer.id(), null));
    }

    /** Переписывает occurred_at событий задачи по порядку их создания (по id). */
    private void setEventTimes(long taskId, Instant... times) {
        List<Long> ids = jdbc.queryForList(
                "select id from task_status_events where task_id = ? order by id", Long.class, taskId);
        assertEquals(times.length, ids.size());

        for (int i = 0; i < times.length; i++) {
            jdbc.update("update task_status_events set occurred_at = ? where id = ?",
                    Timestamp.from(times[i]), ids.get(i));
        }
    }

    @Test
    void statusDurations_sumsRepeatedStatus_andIgnoresAssigneeChange() {
        SkillResponse java = createSkill("Java");
        DeveloperResponse first = createDeveloper("first@example.com", java.id());
        DeveloperResponse second = createDeveloper("second@example.com", java.id());

        TaskResponse task = createTask("Task", java.id());   // событие 1: создание, NEW
        task = assign(task, first);                           // событие 2: NEW -> IN_PROGRESS
        task = assign(task, second);                          // событие 3: смена исполнителя, статус тот же
        task = taskService.unassign(task.id(), task.version(), null); // событие 4: IN_PROGRESS -> NEW
        task = assign(task, first);                           // событие 5: NEW -> IN_PROGRESS

        setEventTimes(task.id(),
                T0,
                T0.plus(2, ChronoUnit.HOURS),
                T0.plus(3, ChronoUnit.HOURS),
                T0.plus(4, ChronoUnit.HOURS),
                T0.plus(5, ChronoUnit.HOURS));

        List<StatusDurationResponse> result =
                reportService.getStatusDurations(task.id(), T0.plus(8, ChronoUnit.HOURS));

        assertEquals(2, result.size());

        // NEW: 0..2ч и 4..5ч, два захода, всего 3ч
        StatusDurationResponse inNew = result.getFirst();
        assertEquals(TaskStatus.NEW, inNew.status());
        assertEquals(2, inNew.timesEntered());
        assertEquals(3 * 3600L, inNew.totalSeconds());
        assertFalse(inNew.current());

        // IN_PROGRESS: 2..4ч и 5..8ч (сейчас), смена исполнителя в 3ч интервал не рвёт, всего 5ч
        StatusDurationResponse inProgress = result.get(1);
        assertEquals(TaskStatus.IN_PROGRESS, inProgress.status());
        assertEquals(2, inProgress.timesEntered());
        assertEquals(5 * 3600L, inProgress.totalSeconds());
        assertTrue(inProgress.current());
    }

    @Test
    void statusDurations_terminalStatus_hasNoDuration() {
        SkillResponse java = createSkill("Java");
        DeveloperResponse dev = createDeveloper("dev@example.com", java.id());
        TaskResponse task = assign(createTask("Task", java.id()), dev);
        taskService.changeStatus(task.id(), new ChangeStatusRequest(task.version(), TaskStatus.DONE, null));

        setEventTimes(task.id(),
                T0,
                T0.plus(1, ChronoUnit.HOURS),
                T0.plus(3, ChronoUnit.HOURS));

        List<StatusDurationResponse> result =
                reportService.getStatusDurations(task.id(), T0.plus(9, ChronoUnit.HOURS));

        StatusDurationResponse inProgress = result.get(1);
        assertEquals(TaskStatus.IN_PROGRESS, inProgress.status());
        assertEquals(2 * 3600L, inProgress.totalSeconds());
        assertFalse(inProgress.current());

        // DONE это конец: время в нём не растёт вместе с «сейчас»
        StatusDurationResponse done = result.get(2);
        assertEquals(TaskStatus.DONE, done.status());
        assertNull(done.totalSeconds());
        assertTrue(done.current());
    }

    @Test
    void statusDurations_unknownTask_throws() {
        assertThrows(ResourceNotFoundException.class,
                () -> reportService.getStatusDurations(999L, T0));
    }
}
