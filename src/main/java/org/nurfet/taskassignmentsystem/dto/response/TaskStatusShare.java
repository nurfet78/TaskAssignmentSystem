package org.nurfet.taskassignmentsystem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;

import java.math.BigDecimal;

@Schema(description = "Доля задач в одном статусе")
public record TaskStatusShare(
        @Schema(description = "Статус", example = "NEW")
        TaskStatus status,

        @Schema(description = "Количество задач", example = "3")
        long count,

        @Schema(description = "Доля от всех задач, %, 1 знак. Из-за округления сумма "
                + "может отличаться от 100", example = "25.0")
        BigDecimal percent
) {
}
