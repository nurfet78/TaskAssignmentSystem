package org.nurfet.taskassignmentsystem.exception;

import lombok.Getter;

import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;

@Getter
public class InvalidStatusTransitionException extends DomainException {

    private final TaskStatus currentStatus;
    private final TaskStatus targetStatus;

    private InvalidStatusTransitionException(TaskStatus currentStatus, TaskStatus targetStatus) {
        super("Недопустимый переход статуса %s в статус %s".formatted(currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public static InvalidStatusTransitionException of(TaskStatus current, TaskStatus target) {
        return new InvalidStatusTransitionException(current, target);
    }
}