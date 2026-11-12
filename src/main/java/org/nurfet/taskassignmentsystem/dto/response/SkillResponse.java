package org.nurfet.taskassignmentsystem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Навык")
public record SkillResponse (

        @Schema(description = "Идентификатор", example = "1")
        Long id,

        @Schema(description = "Название", example = "Java")
        String name,

        @Schema(description = "Описание", example = "Язык программирования Java", nullable = true)
        String description
) {

}