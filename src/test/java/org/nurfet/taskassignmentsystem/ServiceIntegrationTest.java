package org.nurfet.taskassignmentsystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nurfet.taskassignmentsystem.domain.enums.TaskPriority;
import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;
import org.nurfet.taskassignmentsystem.dto.request.*;
import org.nurfet.taskassignmentsystem.dto.response.DeveloperResponse;
import org.nurfet.taskassignmentsystem.dto.response.SkillResponse;
import org.nurfet.taskassignmentsystem.dto.response.TaskEventResponse;
import org.nurfet.taskassignmentsystem.dto.response.TaskResponse;
import org.nurfet.taskassignmentsystem.exception.*;
import org.nurfet.taskassignmentsystem.service.DeveloperService;
import org.nurfet.taskassignmentsystem.service.SkillService;
import org.nurfet.taskassignmentsystem.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    SkillService skillService;
    @Autowired
    DeveloperService developerService;
    @Autowired
    TaskService taskService;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("""
                truncate task_status_events, task_skills, tasks,
                         developer_skills, developers, skills
                restart identity cascade
                """);
    }

    // =====================================================================
    // Хелперы.
    // =====================================================================

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

    private DeveloperResponse setActive(DeveloperResponse developer, boolean active) {
        return developerService.update(developer.id(),
                new UpdateDeveloperRequest(developer.version(), null, null, active, null));
    }

    private TaskResponse changeStatus(TaskResponse task, TaskStatus status) {
        return taskService.changeStatus(task.id(),
                new ChangeStatusRequest(task.version(), status, null));
    }

    // =====================================================================
    // Навыки
    // =====================================================================

    @Test
    void skill_create_stripsName() {
        SkillResponse skill = createSkill("  Kotlin  ");

        assertEquals("Kotlin", skill.name());
    }

    @Test
    void skill_create_duplicateIgnoringCase_throws() {
        createSkill("Java");

        assertThrows(DuplicateResourceException.class, () -> createSkill("jAVA"));
        assertThrows(DuplicateResourceException.class, () -> createSkill("  Java "));
    }

    @Test
    void skill_update_changeOwnCase_isAllowed() {
        SkillResponse java = createSkill("Java");

        SkillResponse updated = skillService.update(java.id(), new UpdateSkillRequest("java", null));

        assertEquals("java", updated.name());
    }

    @Test
    void skill_update_toNameOfAnotherSkill_throws() {
        createSkill("Java");
        SkillResponse spring = createSkill("Spring");

        assertThrows(DuplicateResourceException.class,
                () -> skillService.update(spring.id(), new UpdateSkillRequest(" JAVA ", null)));
    }

    @Test
    void skill_getByName_ignoresCaseAndSpaces() {
        SkillResponse java = createSkill("Java");

        assertEquals(java.id(), skillService.getByName("  jAvA ").id());
    }

    @Test
    void skill_delete_whenUsedByDeveloper_throwsAndKeepsSkill() {
        SkillResponse java = createSkill("Java");
        createDeveloper("dev@example.com", java.id());

        assertThrows(ResourceInUseException.class, () -> skillService.delete(java.id()));

        assertEquals("Java", skillService.getById(java.id()).name());
    }

    @Test
    void skill_delete_whenUsedByTask_throws() {
        SkillResponse java = createSkill("Java");
        createTask("Task", java.id());

        assertThrows(ResourceInUseException.class, () -> skillService.delete(java.id()));
    }

    @Test
    void skill_delete_whenUnused_succeeds() {
        SkillResponse java = createSkill("Java");

        skillService.delete(java.id());

        assertThrows(ResourceNotFoundException.class, () -> skillService.getById(java.id()));
    }

    // =====================================================================
    // Разработчики
    // =====================================================================

    @Test
    void developer_create_normalizesEmail() {
        DeveloperResponse dev = createDeveloper("  John@Example.COM ");

        assertEquals("john@example.com", dev.email());
    }

    @Test
    void developer_create_duplicateEmailIgnoringCase_throws() {
        createDeveloper("john@example.com");

        assertThrows(DuplicateResourceException.class, () -> createDeveloper("JOHN@example.com"));
    }

    @Test
    void developer_deactivate_withActiveTask_throws() {
        SkillResponse java = createSkill("Java");
        DeveloperResponse dev = createDeveloper("dev@example.com", java.id());
        TaskResponse task = createTask("Task", java.id());
        assign(task, dev);

        assertThrows(InvalidDeveloperStateException.class, () -> setActive(dev, false));
    }

    @Test
    void developer_deactivate_withoutTasks_succeeds() {
        DeveloperResponse dev = createDeveloper("dev@example.com");

        DeveloperResponse updated = setActive(dev, false);

        assertFalse(updated.active());
    }

    @Test
    void developer_deactivate_afterTaskDone_succeeds() {
        SkillResponse java = createSkill("Java");
        DeveloperResponse dev = createDeveloper("dev@example.com", java.id());
        TaskResponse task = assign(createTask("Task", java.id()), dev);
        changeStatus(task, TaskStatus.DONE);

        // версия разработчика не менялась, берём актуальную из БД
        DeveloperResponse fresh = developerService.getById(dev.id());

        assertFalse(setActive(fresh, false).active());
    }

    @Test
    void developer_update_withStaleVersion_throws() {
        DeveloperResponse dev = createDeveloper("dev@example.com");
        setActive(dev, true); // версия может измениться

        DeveloperResponse stale = new DeveloperResponse(
                dev.id(), dev.version() - 1, dev.firstName(), dev.lastName(), dev.fullName(),
                dev.email(), dev.active(), dev.skills(), dev.activeTaskCount(),
                dev.createdAt(), dev.updatedAt());

        assertThrows(EntityConcurrentModificationException.class, () -> setActive(stale, true));
    }

    // =====================================================================
    // Задачи
    // =====================================================================

    @Test
    void task_assign_setsInProgressAndAssignee() {
        SkillResponse java = createSkill("Java");
        DeveloperResponse dev = createDeveloper("dev@example.com", java.id());
        TaskResponse task = createTask("Task", java.id());

        TaskResponse assigned = assign(task, dev);

        assertEquals(TaskStatus.IN_PROGRESS, assigned.status());
        assertEquals(dev.id(), assigned.assignee().id());
    }

    @Test
    void task_assign_toInactiveDeveloper_throws() {
        SkillResponse java = createSkill("Java");
        DeveloperResponse dev = createDeveloper("dev@example.com", java.id());
        setActive(dev, false);
        TaskResponse task = createTask("Task", java.id());

        assertThrows(TaskAssignmentFailedException.class, () -> assign(task, dev));
    }

    @Test
    void task_assign_developerWithoutRequiredSkill_throws() {
        SkillResponse java = createSkill("Java");
        SkillResponse go = createSkill("Go");
        DeveloperResponse dev = createDeveloper("dev@example.com", go.id());
        TaskResponse task = createTask("Task", java.id());

        assertThrows(MissingRequiredSkillsException.class, () -> assign(task, dev));
    }

    @Test
    void task_changeStatus_toInProgressWithoutAssignee_throws() {
        TaskResponse task = createTask("Task");

        assertThrows(TaskHasNoAssigneeException.class,
                () -> changeStatus(task, TaskStatus.IN_PROGRESS));
    }

    @Test
    void task_changeStatus_invalidTransition_throws() {
        TaskResponse task = createTask("Task");

        assertThrows(InvalidStatusTransitionException.class,
                () -> changeStatus(task, TaskStatus.DONE));
    }

    @Test
    void task_changeStatus_writesEvent() {
        TaskResponse task = createTask("Task");
        changeStatus(task, TaskStatus.CANCELLED);

        // событие создания + событие отмены
        assertEquals(2, taskService.getHistory(task.id()).size());
    }

    @Test
    void task_update_incrementsVersion() {
        TaskResponse task = createTask("Task");

        TaskResponse updated = taskService.update(task.id(),
                new UpdateTaskRequest(task.version(), "New title", null, null, null));

        assertEquals("New title", updated.title());
        assertTrue(updated.version() > task.version());
    }

    @Test
    void task_update_withStaleVersion_throws() {
        TaskResponse task = createTask("Task");
        taskService.update(task.id(),
                new UpdateTaskRequest(task.version(), "First", null, null, null));

        assertThrows(EntityConcurrentModificationException.class,
                () -> taskService.update(task.id(),
                        new UpdateTaskRequest(task.version(), "Second", null, null, null)));
    }

    @Test
    void task_delete_inProgress_throws() {
        SkillResponse java = createSkill("Java");
        DeveloperResponse dev = createDeveloper("dev@example.com", java.id());
        TaskResponse task = assign(createTask("Task", java.id()), dev);

        assertThrows(InvalidTaskStateException.class, () -> taskService.delete(task.id()));
    }

    @Test
    void task_delete_new_succeedsAndRemovesSkillLinks() {
        SkillResponse java = createSkill("Java");
        TaskResponse task = createTask("Task", java.id());

        taskService.delete(task.id());

        assertThrows(ResourceNotFoundException.class, () -> taskService.getById(task.id()));
        // связи удалены каскадом, навык больше не «используется»
        skillService.delete(java.id());
    }

    // =====================================================================
    // Автоназначение
    // =====================================================================

    @Test
    void autoAssign_skipsInactiveDevelopers() {
        SkillResponse java = createSkill("Java");
        DeveloperResponse inactive = createDeveloper("inactive@example.com", java.id());
        DeveloperResponse active = createDeveloper("active@example.com", java.id());
        setActive(inactive, false);
        TaskResponse task = createTask("Task", java.id());

        taskService.autoAssign(task.id());

        assertEquals(active.id(), taskService.getById(task.id()).assignee().id());
    }

    @Test
    void autoAssign_noActiveCandidates_throws() {
        SkillResponse java = createSkill("Java");
        DeveloperResponse dev = createDeveloper("dev@example.com", java.id());
        setActive(dev, false);
        TaskResponse task = createTask("Task", java.id());

        assertThrows(TaskAssignmentFailedException.class, () -> taskService.autoAssign(task.id()));
    }

    @Test
    void autoAssign_prefersLessLoadedDeveloper() {
        SkillResponse java = createSkill("Java");
        DeveloperResponse busy = createDeveloper("busy@example.com", java.id());
        DeveloperResponse free = createDeveloper("free@example.com", java.id());
        assign(createTask("Busy task", java.id()), busy);
        TaskResponse task = createTask("New task", java.id());

        taskService.autoAssign(task.id());

        assertEquals(free.id(), taskService.getById(task.id()).assignee().id());
    }

    @Test
    void autoAssign_taskWithoutSkills_throws() {
        TaskResponse task = createTask("Task");

        assertThrows(TaskAssignmentFailedException.class, () -> taskService.autoAssign(task.id()));
    }

    @Test
    void getUnassigned_returnsOnlyNewUnassignedTasks() {
        SkillResponse java = createSkill("Java");
        DeveloperResponse dev = createDeveloper("dev@example.com", java.id());
        createTask("Free", java.id());
        assign(createTask("Taken", java.id()), dev);

        var unassigned = taskService.getUnassigned();

        assertEquals(1, unassigned.size());
        assertEquals("Free", unassigned.getFirst().title());
    }

    // =====================================================================
    // Несуществующие id навыков в skillIds
    // =====================================================================

    /**
     * Проверка «ничего не появилось» сделана через SQL, а не через ответы сервиса,
     * потому что именно строки в базе показывают, что транзакция откатилась.
     * Пока SkillService.findByIds не сверяет число найденных навыков,
     * четыре теста с 999L будут падать (задача и разработчик создадутся с неполным набором):
     * так и задумано, они проверяют исправление
     *==============================================================================

     * [java, 999] даёт ResourceNotFoundException, в tasks, task_skills и task_status_events ничего нет
     */

    @Test
    void task_create_withUnknownSkillId_throwsAndCreatesNothing() {
        SkillResponse java = createSkill("Java");

        assertThrows(ResourceNotFoundException.class,
                () -> createTask("Task", java.id(), 999L));

        assertEquals(0, jdbc.queryForObject("select count(*) from tasks", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from task_skills", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from task_status_events", Integer.class));
    }

    /**
     * [998, 999] тоже ошибка, задача без навыков не создаётся
     */
    @Test
    void task_create_withOnlyUnknownSkillIds_throws() {
        assertThrows(ResourceNotFoundException.class,
                () -> createTask("Task", 998L, 999L));

        assertEquals(0, jdbc.queryForObject("select count(*) from tasks", Integer.class));
    }

    /**
     * Контрольный: два существующих навыка сохраняются оба
     */
    @Test
    void task_create_withExistingSkills_savesAllOfThem() {
        SkillResponse java = createSkill("Java");
        SkillResponse spring = createSkill("Spring");

        TaskResponse task = createTask("Task", java.id(), spring.id());

        assertEquals(2, jdbc.queryForObject(
                "select count(*) from task_skills where task_id = ?", Integer.class, task.id()));
    }

    /**
     * Обновление падает, название, версия и навыки задачи не изменились
     */
    @Test
    void task_update_withUnknownSkillId_throwsAndKeepsEverything() {
        SkillResponse java = createSkill("Java");
        TaskResponse task = createTask("Task", java.id());

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.update(task.id(),
                        new UpdateTaskRequest(task.version(), "New title", null, null,
                                Set.of(java.id(), 999L))));

        // откат
        TaskResponse unchanged = taskService.getById(task.id());
        assertEquals("Task", unchanged.title());
        assertEquals(task.version(), unchanged.version());
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from task_skills where task_id = ?", Integer.class, task.id()));
    }

    /**
     * Ошибка, нет строк в developers и developer_skills, тот же email после отката снова свободен
     */
    @Test
    void developer_create_withUnknownSkillId_throwsAndCreatesNothing() {
        SkillResponse java = createSkill("Java");

        assertThrows(ResourceNotFoundException.class,
                () -> createDeveloper("dev@example.com", java.id(), 999L));

        assertEquals(0, jdbc.queryForObject("select count(*) from developers", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from developer_skills", Integer.class));

        DeveloperResponse dev = createDeveloper("dev@example.com", java.id());
        assertEquals("dev@example.com", dev.email());
    }

    /**
     * Ошибка, прежние навыки остались, версия не изменилась
     */

    @Test
    void developer_update_withUnknownSkillId_throwsAndKeepsSkills() {
        SkillResponse java = createSkill("Java");
        SkillResponse spring = createSkill("Spring");
        DeveloperResponse dev = createDeveloper("dev@example.com", java.id());

        assertThrows(ResourceNotFoundException.class,
                () -> developerService.update(dev.id(),
                        new UpdateDeveloperRequest(dev.version(), null, null, null,
                                Set.of(spring.id(), 999L))));

        // прежний набор навыков не потерян и версия не изменилась
        DeveloperResponse unchanged = developerService.getById(dev.id());
        assertEquals(dev.version(), unchanged.version());
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from developer_skills where developer_id = ? and skill_id = ?",
                Integer.class, dev.id(), java.id()));
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from developer_skills where developer_id = ? and skill_id = ?",
                Integer.class, dev.id(), spring.id()));
    }

    @Test
    void task_assign_logsTransitionFromNewToInProgress() {
        SkillResponse java = createSkill("Java");
        DeveloperResponse dev = createDeveloper("dev@wxample.com", java.id());
        TaskResponse task = createTask("Task", java.id());

        assign(task, dev);

        // история отсортирована от новых к старым: первое событие это назначение
        TaskEventResponse last = taskService.getHistory(task.id()).getFirst();
        assertEquals(TaskStatus.NEW, last.previousStatus());
        assertEquals(TaskStatus.IN_PROGRESS, last.newStatus());
    }

    @Test
    void task_unassign_logsTransitionFromInProgressToNew() {
        SkillResponse java = createSkill("Java");
        DeveloperResponse dev = createDeveloper("dev@example.com", java.id());
        TaskResponse assigned = assign(createTask("Task", java.id()), dev);

        taskService.unassign(assigned.id(), assigned.version(), null);

        TaskEventResponse last = taskService.getHistory(assigned.id()).getFirst();
        assertEquals(TaskStatus.IN_PROGRESS, last.previousStatus());
        assertEquals(TaskStatus.NEW, last.newStatus());
    }
}
