package org.nurfet.taskassignmentsystem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Где используется навык")
public record SkillUsageResponse(

        @Schema(description = "Сколько разработчиков владеют навыком", example = "2")
        long developersCount,

        @Schema(description = "Сколько задач требуют навык в любом статусе, "
                + "включая DONE и CANCELLED", example = "3")
        long tasksCount,

        @Schema(description = "true, если удаление будет отклонено (409 resource_in_use)",
                example = "true")
        boolean inUse
) {
}
