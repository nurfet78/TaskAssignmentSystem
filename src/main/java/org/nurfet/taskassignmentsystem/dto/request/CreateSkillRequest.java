package org.nurfet.taskassignmentsystem.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на создание навыка")
public record CreateSkillRequest (

        @Schema(description = "Название навыка. Уникально без учёта регистра, "
                + "пробелы по краям убираются", example = "Java")
        @NotBlank(message = "Укажите название навыка")
        @Size(max = 100, message = "Название навыка не должно превышать 100 символов")
        String name,

        @Schema(description = "Описание навыка", example = "Язык программирования Java")
        @Size(max = 500, message = "Описание не должно превышать 500 символов")
        String description
) {

}