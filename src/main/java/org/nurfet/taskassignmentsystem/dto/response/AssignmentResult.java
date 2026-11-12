package org.nurfet.taskassignmentsystem.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import org.nurfet.taskassignmentsystem.domain.enums.MatchType;

@Schema(description = "Результат автоматического назначения задачи")
public record AssignmentResult (

        @Schema(description = "Идентификатор задачи", example = "6")
        Long taskId,

        @Schema(description = "Идентификатор выбранного разработчика", example = "1")
        Long assigneeId,

        @Schema(description = "Email выбранного разработчика", example = "alex.ivanov@example.com")
        String assigneeEmail,

        @Schema(description = "Тип совпадения: FULL (все требуемые навыки) или PARTIAL (часть навыков)",
                example = "FULL")
        MatchType matchType,

        @Schema(description = "Количество требуемых навыков, которыми владеет разработчик", example = "2")
        long matchingSkillsCount,

        @Schema(description = "Общее количество навыков, требуемых задачей", example = "2")
        long totalRequiredSkills,

        @Schema(description = "Количество активных задач разработчика на момент выбора", example = "2")
        long developerTaskCount,

        String message
) {

    public static AssignmentResult fullMatch(Long taskId, Long developerId, String email,
                                             long skills, long taskCount) {

        String message = """
                Задача назначена разработчику
                с полным совпадением навыков (%d из %d)
                """.formatted(skills, skills);

        return new AssignmentResult(taskId, developerId, email, MatchType.FULL,
                skills, skills, taskCount, message);
    }

    public static AssignmentResult partialMatch(Long taskId, Long developerId, String email,
                                             long matching, long total, long taskCount) {

        String message = """
                Задача назначена разработчику
                с частичным совпадением навыков (%d из %d)
                """.formatted(matching, total);

        return new AssignmentResult(taskId, developerId, email, MatchType.PARTIAL,
                matching, total, taskCount, message);
    }

    @Schema(description = "Признак успешного назначения (исполнитель определён)")
    public boolean isSuccess() {
        return assigneeId != null;
    }
}