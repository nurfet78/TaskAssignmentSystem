package org.nurfet.taskassignmentsystem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.nurfet.taskassignmentsystem.domain.enums.TaskPriority;

import java.math.BigDecimal;

@Schema(description = "Доля задач одного приоритета")
public record PriorityShare(

        @Schema(description = "Приоритет", example = "HIGH")
        TaskPriority priority,

        @Schema(description = "Количество задач в любом статусе", example = "3")
        long count,

        @Schema(description = "Доля от всех задач, %, 1 знак. Из-за округления сумма "
                + "может отличаться от 100", example = "25.0")
        BigDecimal percent
) {
}
