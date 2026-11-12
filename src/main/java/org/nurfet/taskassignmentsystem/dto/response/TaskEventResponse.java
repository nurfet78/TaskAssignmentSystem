package org.nurfet.taskassignmentsystem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;

import java.time.Instant;

@Schema(description = "Событие в истории задачи: создание, смена статуса или смена исполнителя")
public record TaskEventResponse (

        @Schema(description = "Идентификатор события", example = "15")
        Long id,

        @Schema(description = "Идентификатор задачи", example = "6")
        Long taskId,

        @Schema(description = "Статус до события. null для события создания задачи",
                example = "NEW", nullable = true)
        TaskStatus previousStatus,

        @Schema(description = "Статус после события", example = "IN_PROGRESS")
        TaskStatus newStatus,

        @Schema(description = "Идентификатор исполнителя на момент события. null, если исполнителя не было",
                example = "1", nullable = true)
        Long assigneeId,

        @Schema(description = "Email исполнителя на момент события (копия, не меняется при смене email)",
                example = "alex.ivanov@example.com", nullable = true)
        String assigneeEmail,

        @Schema(description = "Дата и время события (UTC)", example = "2026-11-04T09:30:00Z")
        Instant occurredAt,

        @Schema(description = "Комментарий к событию", example = "Взята в работу", nullable = true)
        String comment,

        @Schema(description = "Версия задачи на момент события", example = "1")
        Long taskVersion
) {

}