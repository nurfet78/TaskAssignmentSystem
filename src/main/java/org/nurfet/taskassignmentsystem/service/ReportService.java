package org.nurfet.taskassignmentsystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nurfet.taskassignmentsystem.domain.enums.TaskPriority;
import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;
import org.nurfet.taskassignmentsystem.dto.response.*;
import org.nurfet.taskassignmentsystem.exception.ResourceNotFoundException;
import org.nurfet.taskassignmentsystem.repository.ReportRepository;
import org.nurfet.taskassignmentsystem.repository.TaskRepository;
import org.nurfet.taskassignmentsystem.repository.projection.PriorityCompletionProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final TaskRepository taskRepository;
    private final Clock clock;

    private static BigDecimal percent(long part, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(part * 100)
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    @Transactional
    public TaskStatisticsResponse getTaskStatistics() {
        Map<TaskStatus, Long> counts = new EnumMap<>(TaskStatus.class);
        reportRepository.countTasksByStatus()
                .forEach(row ->
                        counts.put(TaskStatus.valueOf(row.getStatus()), row.getTaskCount()));

        long total = counts.values().stream()
                .mapToLong(Long::longValue).sum();

        List<TaskStatusShare> byStatus = Arrays.stream(TaskStatus.values())
                .map(status -> {
                    long count = counts.getOrDefault(status, 0L);
                    return new TaskStatusShare(status, count, percent(count, total));
                })
                .toList();

        Map<TaskPriority, Long> priorityCounts = new EnumMap<>(TaskPriority.class);
        reportRepository.countTasksByPriority().forEach(row ->
                priorityCounts.put(TaskPriority.valueOf(row.getPriority()), row.getTaskCount()));

        List<PriorityShare> distribution = Arrays.stream(TaskPriority.values())
                .sorted(Comparator.comparingInt(TaskPriority::getWeight).reversed())
                .map(priority -> {
                    long count = priorityCounts.getOrDefault(priority, 0L);
                    return new PriorityShare(priority, count, percent(count, total));
                })
                .toList();

        Map<TaskPriority, PriorityCompletionProjection> completion = new EnumMap<>(TaskPriority.class);
        reportRepository.findCompletionStatsByPriority().forEach(row ->
                completion.put(TaskPriority.valueOf(row.getPriority()), row));

        List<PriorityCompletionStats> byPriority = Arrays.stream(TaskPriority.values())
                .sorted(Comparator.comparingInt(TaskPriority::getWeight).reversed())
                .map(priority -> {
                    PriorityCompletionProjection row = completion.get(priority);
                    return row == null
                            ? new PriorityCompletionStats(priority, 0, null, null, null)
                            : new PriorityCompletionStats(priority, row.getCompletedTasks(),
                            row.getAvgHours(), row.getMinHours(), row.getMaxHours());
                })
                .toList();

        return new TaskStatisticsResponse(total, byStatus, distribution, byPriority);
    }

    @Transactional(readOnly = true)
    public List<UnassignableTaskResponse> getUnassignableTasks() {
        return reportRepository.findUnassignableTasks().stream()
                .map(row -> new UnassignableTaskResponse(
                        row.getTaskId(),
                        row.getTitle(),
                        TaskPriority.valueOf(row.getPriority()),
                        row.getRequiredSkillsCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StatusDurationResponse> getStatusDurations(Long taskId) {
        return getStatusDurations(taskId, Instant.now(clock));
    }

    /** Вариант с явным «сейчас»: нужен тестам с точными числами. */
    @Transactional(readOnly = true)
    public List<StatusDurationResponse> getStatusDurations(Long taskId, Instant now) {
        if (!taskRepository.existsById(taskId)) {
            throw ResourceNotFoundException.task(taskId);
        }

        return reportRepository.findStatusDurations(taskId, now).stream()
                .map(row -> new StatusDurationResponse(
                        TaskStatus.valueOf(row.getStatus()),
                        row.getTimesEntered(),
                        row.getTotalSeconds(),
                        Boolean.TRUE.equals(row.getIsCurrent())))
                .toList();
    }

}
