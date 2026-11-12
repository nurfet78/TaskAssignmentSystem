package org.nurfet.taskassignmentsystem.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.Set;

@Schema(enumAsRef = true, description = """
        Статус задачи:
        NEW: создана, исполнителя нет;
        IN_PROGRESS: в работе, исполнитель назначен;
        DONE: завершена (конечный статус);
        CANCELLED: отменена (конечный статус).
        Допустимые переходы: NEW -> IN_PROGRESS (только через назначение), NEW -> CANCELLED,
        IN_PROGRESS -> DONE, IN_PROGRESS -> CANCELLED.
        Возврат IN_PROGRESS -> NEW происходит при снятии исполнителя""")
public enum TaskStatus {
    NEW,
    IN_PROGRESS,
    DONE,
    CANCELLED;

    public boolean canTransitionTo(TaskStatus target) {
        if (target == this) {
            return false;
        }

        return switch (this) {
            case NEW -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == DONE || target == CANCELLED;
            case DONE, CANCELLED -> false;
        };
    }

    public boolean isActive() {
        return this == NEW || this == IN_PROGRESS;
    }

    public boolean isTerminal() {
        return this == DONE || this == CANCELLED;
    }

    public boolean isDeletable() {
        return this == NEW || this == CANCELLED;
    }

    public static final Set<TaskStatus> ACTIVE = Set.copyOf(
            Arrays.stream(values())
                    .filter(TaskStatus::isActive)
                    .toList());
}