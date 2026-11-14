package org.nurfet.taskassignmentsystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nurfet.taskassignmentsystem.domain.enums.TaskPriority;
import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;
import org.nurfet.taskassignmentsystem.dto.response.PriorityCompletionStats;
import org.nurfet.taskassignmentsystem.dto.response.TaskStatisticsResponse;
import org.nurfet.taskassignmentsystem.dto.response.TaskStatusShare;
import org.nurfet.taskassignmentsystem.dto.response.UnassignableTaskResponse;
import org.nurfet.taskassignmentsystem.repository.ReportRepository;
import org.nurfet.taskassignmentsystem.repository.projection.PriorityCompletionProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

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

        return new TaskStatisticsResponse(total, byStatus, byPriority);
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
}
