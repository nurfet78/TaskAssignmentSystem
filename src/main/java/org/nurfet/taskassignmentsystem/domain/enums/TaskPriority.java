package org.nurfet.taskassignmentsystem.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(enumAsRef = true, description = """
        Приоритет задачи от низкого к высокому: LOW, MEDIUM, HIGH, CRITICAL.
        Список неназначенных задач сортируется от CRITICAL к LOW""")
@Getter
public enum TaskPriority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int weight;

    TaskPriority(int weight) {
        this.weight = weight;
    }
}