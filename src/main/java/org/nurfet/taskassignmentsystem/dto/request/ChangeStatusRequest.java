package org.nurfet.taskassignmentsystem.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;

@Schema(description = "Запрос на смену статуса задачи")
public record ChangeStatusRequest (

        @Schema(description = "Текущая версия задачи (оптимистическая блокировка). "
                + "Берётся из последнего ответа сервера", example = "1")
        @NotNull(message = "Версия обязательна для оптимистической блокировки")
        Long version,

        @Schema(description = "Новый статус. Перевод в IN_PROGRESS возможен только "
                + "через назначение исполнителя", example = "DONE")
        @NotNull(message = "Укажите новый статус задачи")
        TaskStatus newStatus,

        @Schema(description = "Комментарий к смене статуса, попадает в историю задачи",
                example = "Работа завершена")
        @Size(max = 500, message = "Комментарий не должен превышать 500 символов")
        String comment
) {

}