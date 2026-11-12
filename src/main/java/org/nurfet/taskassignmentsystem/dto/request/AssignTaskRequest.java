package org.nurfet.taskassignmentsystem.dto.request;

// DTO для ручного назначения задачи разработчику

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на ручное назначение задачи разработчику")
public record AssignTaskRequest (

        @Schema(description = "Текущая версия задачи (оптимистическая блокировка). "
                + "Берётся из последнего ответа сервера", example = "0")
        @NotNull(message = "Версия обязательна для оптимистической блокировки")
        Long version,

        @Schema(description = "Идентификатор разработчика, которому назначается задача", example = "1")
        @NotNull(message = "Укажите идентификатор разработчика")
        Long developerId,

        @Schema(description = "Комментарий к назначению, попадает в историю задачи",
                example = "Назначено вручную")
        @Size(max = 500, message = "Комментарий не должен превышать 500 символов")

        String comment
) {

}