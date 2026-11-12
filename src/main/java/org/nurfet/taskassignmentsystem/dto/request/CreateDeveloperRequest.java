package org.nurfet.taskassignmentsystem.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.nurfet.taskassignmentsystem.validation.ValidEmail;

import java.util.Set;

@Schema(description = "Запрос на создание разработчика")
public record CreateDeveloperRequest (

        @Schema(description = "Имя", example = "Иван")
        @NotBlank(message = "Укажите имя")
        @Size(max = 100, message = "Имя не должно превышать 100 символов")
        String firstName,

        @Schema(description = "Фамилия", example = "Петров")
        @NotBlank(message = "Укажите фамилию")
        @Size(max = 100, message = "Фамилия не должна превышать 100 символов")
        String lastName,

        @Schema(description = "Email. Уникален без учёта регистра, хранится в нижнем регистре, "
                + "пробелы по краям недопустимы", example = "ivan.petrov@example.com")
        @NotBlank(message = "Поле email должно быть заполнено")
        @ValidEmail
        String email,

        @Schema(description = "Идентификаторы навыков разработчика. "
                + "Необязательное поле; несуществующий навык приводит к ошибке 404",
                example = "[1, 2]")
        Set<Long> skillIds
) {

}