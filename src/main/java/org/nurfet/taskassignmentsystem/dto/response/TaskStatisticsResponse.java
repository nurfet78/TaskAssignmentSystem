package org.nurfet.taskassignmentsystem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Общая статистика по задачам")
public record TaskStatisticsResponse(
        @Schema(description = "Всего задач", example = "12")
        long totalTasks,

        @Schema(description = "Распределение по статусам. Присутствуют все статусы, в том числе с нулём задач")
        List<TaskStatusShare> byStatus,

        @Schema(description = "Время выполнения по приоритетам, от CRITICAL к LOW. "
                + "Учитываются только задачи в статусе DONE; отменённые не входят, "
                + "хотя у них тоже заполнен completedAt")
        List<PriorityCompletionStats> completionByPriority
) {
}
