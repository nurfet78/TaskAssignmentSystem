package org.nurfet.taskassignmentsystem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;

@Schema(description = "Разработчик")
public record DeveloperResponse (

        @Schema(description = "Идентификатор", example = "1")
        Long id,

        @Schema(description = "Версия для оптимистической блокировки. "
                + "Передаётся в запросах на изменение", example = "0")
        Long version,

        @Schema(description = "Имя", example = "Алексей")
        String firstName,

        @Schema(description = "Фамилия", example = "Иванов")
        String lastName,

        @Schema(description = "Полное имя", example = "Алексей Иванов")
        String fullName,

        @Schema(description = "Email в нижнем регистре", example = "alex.ivanov@example.com")
        String email,

        @Schema(description = "Признак активности. Неактивному разработчику нельзя назначить задачу",
                example = "true")
        boolean active,

        @Schema(description = "Навыки разработчика")
        Set<SkillResponse> skills,

        @Schema(description = "Количество активных задач (в статусах NEW и IN_PROGRESS с исполнителем)",
                example = "2")
        long activeTaskCount,

        @Schema(description = "Дата и время создания (UTC)", example = "2026-11-04T09:00:00Z")
        Instant createdAt,

        @Schema(description = "Дата и время последнего изменения (UTC)", example = "2026-11-04T09:00:00Z")
        Instant updatedAt
) {

}