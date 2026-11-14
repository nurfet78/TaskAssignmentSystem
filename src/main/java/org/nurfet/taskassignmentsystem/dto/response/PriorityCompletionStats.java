package org.nurfet.taskassignmentsystem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.nurfet.taskassignmentsystem.domain.enums.TaskPriority;

import java.math.BigDecimal;

@Schema(description = "Скорость закрытия задач одного приоритета")
public record PriorityCompletionStats(
        @Schema(description = "Приоритет", example = "HIGH")
        TaskPriority priority,

        @Schema(description = "Количество задач в статусе DONE", example = "2")
        long completedTasks,

        @Schema(description = "Среднее время от создания до завершения, часов, 1 знак. "
                + "null, если выполненных задач нет", example = "3.0", nullable = true)
        BigDecimal avgHours,

        @Schema(description = "Минимальное время, часов, 1 знак", example = "2.0", nullable = true)
        BigDecimal minHours,

        @Schema(description = "Максимальное время, часов, 1 знак", example = "4.0", nullable = true)
        BigDecimal maxHours
) {
}
