package org.nurfet.taskassignmentsystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nurfet.taskassignmentsystem.domain.entity.Developer;
import org.nurfet.taskassignmentsystem.domain.entity.Task;
import org.nurfet.taskassignmentsystem.domain.entity.TaskStatusEvent;
import org.nurfet.taskassignmentsystem.domain.enums.MatchType;
import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;
import org.nurfet.taskassignmentsystem.dto.response.AssignmentResult;
import org.nurfet.taskassignmentsystem.dto.response.CandidateResponse;
import org.nurfet.taskassignmentsystem.exception.ResourceNotFoundException;
import org.nurfet.taskassignmentsystem.exception.TaskAssignmentFailedException;
import org.nurfet.taskassignmentsystem.repository.DeveloperRepository;
import org.nurfet.taskassignmentsystem.repository.TaskRepository;
import org.nurfet.taskassignmentsystem.repository.TaskStatusEventRepository;
import org.nurfet.taskassignmentsystem.repository.projection.DeveloperMatchProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис автоматического распределения задач.
 *
 * Алгоритм:
 * 1. Ищем разработчиков с ПОЛНЫМ совпадением всех требуемых навыков
 * 2. Если нет — ищем с ЧАСТИЧНЫМ совпадением (≥1 навык)
 * 3. Из найденных выбираем с минимальной загрузкой (количество активных задач)
 *
 * Все операции выполняются в одной транзакции для обеспечения консистентности.
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskAssignmentService {

    private final TaskRepository taskRepository;
    private final DeveloperRepository developerRepository;
    private final TaskStatusEventRepository eventRepository;

    private void recordAssigneeChange(Task task, TaskStatus previousStatus,
                                      Developer previousAssignee, String comment) {
        taskRepository.saveAndFlush(task);
        eventRepository.save(TaskStatusEvent.assigneeChanged(task, previousStatus, previousAssignee, comment));
    }

    private AssignmentResult applyAutoAssignment(Task task, DeveloperMatchProjection match) {
        boolean fullMatch = Boolean.TRUE.equals(match.getIsExactMatch());

        MatchType matchType = fullMatch
                ? MatchType.FULL
                : MatchType.PARTIAL;

        Developer developer = developerRepository.findByIdWithLock(match.getDeveloperId())
                .orElseThrow(() -> ResourceNotFoundException.developer(match.getDeveloperId()));

        log.info("Назначение задачи {} разработчику {} ({} совпадение {}/{} навыков {} активных задач)",
                task.getId(),
                match.getDeveloperId(),
                matchType,
                match.getMatchedSkills(),
                match.getRequiredSkills(),
                match.getActiveTasks());

        TaskStatus previousStatus = task.getStatus();
        Developer previousAssignee = task.getAssignee();
        task.assignTo(developer);
        recordAssigneeChange(task, previousStatus, previousAssignee,
                "Автоматическое назначение (" + matchType + " совпадение навыков)");

        return matchType == MatchType.FULL
                ?
                AssignmentResult.fullMatch(
                        task.getId(),
                        match.getDeveloperId(),
                        match.getEmail(),
                        match.getMatchedSkills(),
                        match.getActiveTasks())
                :
                AssignmentResult.partialMatch(
                        task.getId(),
                        match.getDeveloperId(),
                        match.getEmail(),
                        match.getMatchedSkills(),
                        match.getRequiredSkills(),
                        match.getActiveTasks());
    }

    private void validateForTask(Task task) {
        if (task.getStatus() != TaskStatus.NEW) {
            throw TaskAssignmentFailedException.taskNotNew(task.getId());
        }

        if (task.isAssigned()) {
            throw TaskAssignmentFailedException.alreadyAssigned(task.getId());
        }

        if (task.getRequiredSkills().isEmpty()) {
            throw TaskAssignmentFailedException.noRequiredSkills(task.getId());
        }
    }

    @Transactional
    public AssignmentResult autoAssign(Task task) {
        log.info("Автоматическое назначение задачи: {}", task.getTitle());

        validateForTask(task);

        List<DeveloperMatchProjection> candidates =
                developerRepository.findBestCandidatesForTask(task.getId(), 1);

        if (candidates.isEmpty()) {
            throw TaskAssignmentFailedException.noMatchingDeveloper(task.getId());
        }

        return applyAutoAssignment(task, candidates.getFirst());
    }

    @Transactional
    public void manualAssign(Task task, Developer developer, String comment) {
        log.info("Ручное назначение задачи {} разработчику: {}", task.getTitle(), developer.getId());

        TaskStatus previousStatus = task.getStatus();
        Developer previousAssignee = task.getAssignee();
        task.assignTo(developer);
        recordAssigneeChange(task, previousStatus, previousAssignee, comment);

        log.info("Задача {} назначена разработчику {}", task.getId(), developer.getId());
    }

    @Transactional
    public void unassign(Task task, String comment) {
        if (!task.isAssigned()) {
            return;
        }

        TaskStatus previousStatus = task.getStatus();
        Developer previousAssignee = task.getAssignee();
        task.unassign();
        recordAssigneeChange(task, previousStatus, previousAssignee, comment);

        log.info("С задачи {} снят исполнитель {}", task.getId(), previousAssignee.getId());
    }

    @Transactional(readOnly = true)
    public List<CandidateResponse> findCandidates(Long taskId, int limit) {
        return developerRepository.findBestCandidatesForTask(taskId, limit).stream()
                .map(c -> new CandidateResponse(
                        c.getDeveloperId(),
                        c.getFirstName() + " " + c.getLastName(),
                        c.getEmail(),
                        Boolean.TRUE.equals(c.getIsExactMatch()) ? MatchType.FULL : MatchType.PARTIAL,
                        c.getMatchedSkills(),
                        c.getRequiredSkills(),
                        c.getActiveTasks()))
                .toList();
    }
}