package org.nurfet.taskassignmentsystem.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.nurfet.taskassignmentsystem.domain.enums.TaskPriority;

import java.util.Set;

@Schema(description = "Запрос на частичное обновление задачи. Поля со значением null не изменяются")
public record UpdateTaskRequest (

        @Schema(description = "Текущая версия задачи (оптимистическая блокировка). "
                + "Берётся из последнего ответа сервера", example = "0")
        @NotNull(message = "Версия обязательна для оптимистической блокировки")
        Long version,

        @Schema(description = "Новое название задачи", example = "Настроить логирование")
        @Pattern(
                regexp = "^\\s*\\S.*$",
                message = "Название задачи не должно быть пустым"
        )
        @Size(max = 200, message = "Название задачи не должно превышать 200 символов")
        String title,

        @Schema(description = "Новое описание задачи")
        @Size(max = 2000, message = "Описание не должно превышать 2000 символов")
        String description,

        @Schema(description = "Новый приоритет", example = "CRITICAL")
        TaskPriority priority,

        @Schema(description = "Новый набор требуемых навыков. null: не менять, "
                + "пустой набор: удалить все навыки", example = "[1, 2]")
        Set<Long> skillIds
) {

}