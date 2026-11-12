package org.nurfet.taskassignmentsystem.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.nurfet.taskassignmentsystem.domain.enums.TaskPriority;

import java.util.Objects;
import java.util.Set;

@Schema(description = "Запрос на создание задачи. Новая задача всегда создаётся в статусе NEW")
public record CreateTaskRequest (

        @Schema(description = "Название задачи", example = "Настроить CI/CD pipeline")
        @NotBlank(message = "Укажите название задачи")
        @Size(max = 200, message = "Название задачи не должно превышать 200 символов")
        String title,

        @Schema(description = "Описание задачи",
                example = "Настроить GitHub Actions: сборка, тесты, деплой в Docker")
        @Size(max = 2000, message = "Описание задачи не должно превышать 2000 символов")
        String description,

        @Schema(description = "Приоритет. Если не указан, используется MEDIUM",
                example = "HIGH", defaultValue = "MEDIUM")
        TaskPriority priority,

        @Schema(description = "Идентификаторы требуемых навыков. Необязательное поле, но без навыков "
                + "задачу нельзя назначить автоматически; несуществующий навык приводит к ошибке 404",
                example = "[1, 2]")
        Set<Long> skillIds
) {
    public CreateTaskRequest {
        priority = Objects.requireNonNullElse(priority, TaskPriority.MEDIUM);
    }
}