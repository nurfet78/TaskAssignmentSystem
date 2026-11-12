package org.nurfet.taskassignmentsystem.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.nurfet.taskassignmentsystem.validation.ValidEmail;

@Schema(description = "Запрос на смену email разработчика")
public record ChangeEmailRequest (

        @Schema(description = "Текущая версия разработчика (оптимистическая блокировка). "
                + "Берётся из последнего ответа сервера", example = "0")
        @NotNull(message = "Версия обязательна для оптимистической блокировки")
        Long version,

        @Schema(description = "Новый email. Хранится в нижнем регистре, "
                + "пробелы по краям недопустимы", example = "ivan.petrov@example.com")
        @NotBlank(message = "Поле email должно быть заполнено")
        @ValidEmail
        String newEmail
) {

}