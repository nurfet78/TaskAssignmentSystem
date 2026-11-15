package org.nurfet.taskassignmentsystem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.nurfet.taskassignmentsystem.domain.enums.MatchType;

@Schema(description = "Кандидат на задачу в порядке предпочтения автоназначения")
public record CandidateResponse(

        @Schema(description = "Идентификатор разработчика", example = "1")
        Long developerId,

        @Schema(description = "Полное имя", example = "Алексей Иванов")
        String fullName,

        @Schema(description = "Email", example = "alex.ivanov@example.com")
        String email,

        @Schema(description = "Тип совпадения: FULL (все навыки) или PARTIAL (часть)", example = "FULL")
        MatchType matchType,

        @Schema(description = "Сколько требуемых навыков есть у разработчика", example = "2")
        long matchedSkills,

        @Schema(description = "Сколько навыков требует задача", example = "2")
        long requiredSkills,

        @Schema(description = "Активных задач у разработчика сейчас", example = "1")
        long activeTasks
) {
}
