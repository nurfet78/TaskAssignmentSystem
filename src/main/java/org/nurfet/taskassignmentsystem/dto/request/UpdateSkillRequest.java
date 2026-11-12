package org.nurfet.taskassignmentsystem.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на частичное обновление навыка. Поля со значением null не изменяются")
public record UpdateSkillRequest(

        @Schema(description = "Новое название. Уникально без учёта регистра", example = "Kotlin")
        @Pattern(regexp = ".*\\S.*", message = "Название навыка не может быть пустым")
        @Size(max = 100, message = "Название навыка не должно превышать 100 символов")
        String name,

        @Schema(description = "Новое описание", example = "Язык программирования Kotlin")
        @Size(max = 500, message = "Описание не должно превышать 500 символов")
        String description
) {
}
