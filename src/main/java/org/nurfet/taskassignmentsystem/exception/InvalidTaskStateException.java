package org.nurfet.taskassignmentsystem.exception;

import lombok.Getter;
import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;

@Getter
public class InvalidTaskStateException extends DomainException {

    private final Long taskId;
    private final TaskStatus currentStatus;

    private InvalidTaskStateException(String message, Long taskId, TaskStatus currentStatus) {
        super(message);
        this.taskId = taskId;
        this.currentStatus = currentStatus;
    }

    public static InvalidTaskStateException cannotDelete(Long taskId, TaskStatus status) {
        return new InvalidTaskStateException(
                "Задачу с id=%d нельзя удалить в статусе %s".formatted(taskId, status),
                taskId,
                status
        );
    }
}
