package org.nurfet.taskassignmentsystem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.nurfet.taskassignmentsystem.domain.enums.TaskPriority;
import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;

import java.time.Instant;
import java.util.Set;

@Schema(description = "Задача")
public record TaskResponse(

        @Schema(description = "Идентификатор", example = "6")
        Long id,

        @Schema(description = "Версия для оптимистической блокировки. "
                + "Передаётся в запросах на изменение", example = "0")
        Long version,

        @Schema(description = "Название", example = "Оптимизировать SQL запросы")
        String title,

        @Schema(description = "Описание", nullable = true)
        String description,

        @Schema(description = "Статус", example = "NEW")
        TaskStatus status,

        @Schema(description = "Приоритет", example = "MEDIUM")
        TaskPriority priority,

        @Schema(description = "Исполнитель. null, пока задача не назначена", nullable = true)
        DeveloperSummary assignee,

        @Schema(description = "Требуемые навыки")
        Set<SkillResponse> requiredSkills,

        @Schema(description = "Дата и время создания (UTC)", example = "2026-11-04T09:00:00Z")
        Instant createdAt,

        @Schema(description = "Дата и время последнего изменения (UTC)", example = "2026-11-04T09:00:00Z")
        Instant updatedAt,

        @Schema(description = "Дата и время назначения исполнителя (UTC). null, если не назначена",
                example = "2026-11-04T09:30:00Z", nullable = true)
        Instant assignedAt,

        @Schema(description = "Дата и время завершения (UTC). null, пока задача не завершена",
                example = "2026-11-05T09:30:00Z", nullable = true)
        Instant completedAt
) {

}