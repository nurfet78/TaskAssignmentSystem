package org.nurfet.taskassignmentsystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nurfet.taskassignmentsystem.domain.entity.Developer;
import org.nurfet.taskassignmentsystem.domain.entity.Skill;
import org.nurfet.taskassignmentsystem.domain.entity.Task;
import org.nurfet.taskassignmentsystem.domain.entity.TaskStatusEvent;
import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;
import org.nurfet.taskassignmentsystem.dto.request.*;
import org.nurfet.taskassignmentsystem.dto.response.AssignmentResult;
import org.nurfet.taskassignmentsystem.dto.response.PageResponse;
import org.nurfet.taskassignmentsystem.dto.response.TaskEventResponse;
import org.nurfet.taskassignmentsystem.dto.response.TaskResponse;
import org.nurfet.taskassignmentsystem.exception.EntityConcurrentModificationException;
import org.nurfet.taskassignmentsystem.exception.InvalidTaskStateException;
import org.nurfet.taskassignmentsystem.exception.ResourceNotFoundException;
import org.nurfet.taskassignmentsystem.mapper.TaskEventMapper;
import org.nurfet.taskassignmentsystem.mapper.TaskMapper;
import org.nurfet.taskassignmentsystem.repository.TaskRepository;
import org.nurfet.taskassignmentsystem.repository.TaskStatusEventRepository;
import org.nurfet.taskassignmentsystem.util.Validate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Set;

/**
 * Основной сервис управления задачами.
 *
 * Обеспечивает:
 * - CRUD операции
 * - Управление статусами с Event Sourcing
 * - Оптимистичную блокировку
 * - Интеграцию с автоматическим распределением

 /////////////////////////////////////////////////////////////////////

 TaskService — фасад над задачами, точка входа для контроллера.
 Его методы autoAssign, assignToDeveloper, unassign выполняют то, что относится к уровню API:

 - загружают задачу (findById / findByIdWithDetails);
 - проверяют версию для оптимистичной блокировки;
 - загружают Developer через DeveloperService;
 - маппят результат в TaskResponse.

 TaskAssignmentService — доменная логика назначения:

 - валидация (validateForAssignment);
 - выбор кандидата (findBestCandidatesForTask);
 - смена исполнителя (task.assignTo);
 - запись события Event Sourcing.

 То есть TaskService не реализует назначение сам, а вызывает assignmentService.
 Это разделение ответственности: «оркестрация + контракт API» и «правила назначения»
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskStatusEventRepository eventRepository;
    private final SkillService skillService;
    private final DeveloperService developerService;
    private final TaskAssignmentService assignmentService;
    private final TaskMapper taskMapper;
    private final TaskEventMapper eventMapper;

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "title", "createdAt", "updatedAt", "assignedAt", "completedAt");

    Task findById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.task(id));
    }

    Task findByIdWithDetails(Long id) {
        return taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> ResourceNotFoundException.task(id));
    }

    private void checkVersion(Task task, Long version) {
        if (!task.getVersion().equals(version)) {
            throw EntityConcurrentModificationException.task(task.getId());
        }
    }

    /**
     * Формирует ответ после изменения задачи.
     * <p>
     * JPQL-запрос заставляет Hibernate сделать flush, поэтому в ответ попадает
     * актуальная версия (@Version), а не значение до записи. Заодно подгружаются
     * связи, нужные для маппинга. Использовать после операций, меняющих задачу;
     * для обычного чтения достаточно findByIdWithDetails + toResponse.
     */
    private TaskResponse reloadResponse(Long id) {
        return taskMapper.toResponse(findByIdWithDetails(id));
    }

    private TaskResponse changeStatusInternal(Task task, TaskStatus newStatus, String comment) {
        TaskStatus previousStatus = task.getStatus();

        task.transitionTo(newStatus);
        taskRepository.saveAndFlush(task);

        TaskStatusEvent event = TaskStatusEvent.fromTask(task, previousStatus, comment);
        eventRepository.save(event);

        log.info("Статус задачи {} изменен: {} -> {}", task.getId(), previousStatus, newStatus);

        return reloadResponse(task.getId());
    }

    /*
	version и createdAt могут оказаться ещё не заполнены, пока не было flush
	поэтому нужно вызвать taskRepository.saveAndFlush(task) или reloadResponse
	*/
    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        log.info("Создание задачи: {}", request.title());

        Task task = taskMapper.toEntity(request);

        if (request.skillIds() != null && !request.skillIds().isEmpty()) {
            Set<Skill> skills = skillService.findByIds(request.skillIds());
            skills.forEach(task::addSkill);
        }

        task = taskRepository.save(task);

        TaskStatusEvent event = TaskStatusEvent.taskCreated(task);
        eventRepository.save(event);

        log.info("Задача создана: id={}, title={}", task.getId(), task.getTitle());

        return reloadResponse(task.getId());
    }

    /*
	после flush подгружает связи и возвращает актуальное состояние

	reloadResponse грузит requiredSkills и assignee одним запросом,
	и ответ собирается без ленивых подгрузок
	*/
    @Transactional
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        log.info("Обновление задачи: {}", id);

        Task task = findById(id);
        checkVersion(task, request.version());

        task.update(request.title(), request.description(), request.priority());

        if (request.skillIds() != null) {
            task.replaceRequiredSkills(skillService.findByIds(request.skillIds()));
        }

        taskRepository.flush();
        log.info("Задача {} обновлена", task.getId());

        return reloadResponse(task.getId());
    }

    @Transactional
    public TaskResponse changeStatus(Long id, ChangeStatusRequest request) {
        log.info("Изменение статуса задачи {} -> {}", id, request.newStatus());

        Task task = findById(id);
        checkVersion(task, request.version());

        return changeStatusInternal(task, request.newStatus(), request.comment());
    }

    @Transactional
    public AssignmentResult autoAssign(Long taskId) {
        Task task = findByIdWithDetails(taskId);
        return assignmentService.autoAssign(task);
    }

    @Transactional
    public TaskResponse assignToDeveloper(Long id, AssignTaskRequest request) {
        Task task = findById(id);
        checkVersion(task, request.version());

        Developer developer = developerService.findByIdWithLock(request.developerId());

        assignmentService.manualAssign(task, developer, request.comment());

        return reloadResponse(id);
    }

    @Transactional
    public TaskResponse unassign(Long id, Long version, String comment) {
        Task task = findById(id);
        checkVersion(task, version);

        assignmentService.unassign(task, comment);

        return reloadResponse(id);
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(Long id) {
        return taskMapper.toResponse(findByIdWithDetails(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getAll(TaskStatus status, Pageable pageable) {
        Validate.validateSort(pageable, SORTABLE_FIELDS);

        Page<Task> page = (status != null)
                ? taskRepository.findByStatus(status, pageable)
                : taskRepository.findPage(pageable);

        return PageResponse.of(taskMapper.toResponseList(page.getContent()), page);
    }


    @Transactional(readOnly = true)
    public List<TaskResponse> getUnassigned() {
        return taskMapper.toResponseList(taskRepository.findUnassignedOrderByPriority());
    }

    @Transactional(readOnly = true)
    public List<TaskEventResponse> getHistory(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw ResourceNotFoundException.task(taskId);
        }

        return eventMapper.toResponseList(eventRepository.findByTaskIdOrderByOccurredAtDesc(taskId));
    }

    @Transactional
    public void delete(Long id) {
        Task task = findById(id);

        if (!task.getStatus().isDeletable()) {
            throw InvalidTaskStateException.cannotDelete(id, task.getStatus());
        }

        taskRepository.delete(task);
        log.info("Задача {} удалена", id);
    }
}