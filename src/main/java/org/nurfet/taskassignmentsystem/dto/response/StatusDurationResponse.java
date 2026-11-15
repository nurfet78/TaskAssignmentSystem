package org.nurfet.taskassignmentsystem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;

@Schema(description = "Время задачи в одном статусе (сумма по всем заходам в этот статус)")
public record StatusDurationResponse(

        @Schema(description = "Статус", example = "IN_PROGRESS")
        TaskStatus status,

        @Schema(description = "Сколько раз задача входила в статус. Смена исполнителя "
                + "без смены статуса новым входом не считается", example = "2")
        long timesEntered,

        @Schema(description = "Суммарное время в статусе, секунд. Для текущего статуса "
                + "считается до момента запроса. null для конечных статусов "
                + "(DONE, CANCELLED): там задача остаётся навсегда", example = "21600",
                nullable = true)
        Long totalSeconds,

        @Schema(description = "Признак того, что задача находится в этом статусе сейчас",
                example = "true")
        boolean current
) {
}
