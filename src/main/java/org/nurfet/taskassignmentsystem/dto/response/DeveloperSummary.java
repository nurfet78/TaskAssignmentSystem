package org.nurfet.taskassignmentsystem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Краткие сведения о разработчике (исполнитель задачи)")
public record DeveloperSummary (

        @Schema(description = "Идентификатор", example = "1")
        Long id,

        @Schema(description = "Полное имя", example = "Алексей Иванов")
        String fullName,

        @Schema(description = "Email", example = "alex.ivanov@example.com")
        String email
) {

}