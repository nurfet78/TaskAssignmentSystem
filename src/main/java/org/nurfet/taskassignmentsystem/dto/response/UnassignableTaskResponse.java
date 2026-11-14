package org.nurfet.taskassignmentsystem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.nurfet.taskassignmentsystem.domain.enums.TaskPriority;

@Schema(description = "Задача, которую нельзя назначить автоматически: подходящего исполнителя нет")
public record UnassignableTaskResponse(

        @Schema(description = "Идентификатор задачи", example = "11")
        Long taskId,

        @Schema(description = "Название", example = "Миграция на Kotlin")
        String title,

        @Schema(description = "Приоритет", example = "HIGH")
        TaskPriority priority,

        @Schema(description = "Количество требуемых навыков", example = "2")
        long requiredSkillsCount
) {
}
