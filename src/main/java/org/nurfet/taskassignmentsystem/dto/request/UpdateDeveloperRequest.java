package org.nurfet.taskassignmentsystem.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Запрос на частичное обновление разработчика. "
        + "Поля со значением null не изменяются")
public record UpdateDeveloperRequest (

        @Schema(description = "Текущая версия разработчика (оптимистическая блокировка). "
                + "Берётся из последнего ответа сервера", example = "0")
        @NotNull(message = "Версия обязательна для оптимистической блокировки")
        Long version,

        @Schema(description = "Новое имя", example = "Иван")
        @Size(max = 100, message = "Имя не должно превышать 100 символов")
        String firstName,

        @Schema(description = "Новая фамилия", example = "Сидоров")
        @Size(max = 100, message = "Фамилия не должна превышать 100 символов")
        String lastName,

        @Schema(description = "Признак активности. Деактивация (false) запрещена, "
                + "пока у разработчика есть активные задачи", example = "true")
        Boolean active,

        @Schema(description = "Новый набор навыков. null: не менять, пустой набор: удалить все навыки",
                example = "[1, 3, 4]")
        Set<Long> skillIds
) {

}