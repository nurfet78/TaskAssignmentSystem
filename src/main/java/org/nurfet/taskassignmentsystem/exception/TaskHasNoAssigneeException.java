package org.nurfet.taskassignmentsystem.exception;

import lombok.Getter;

@Getter
public class TaskHasNoAssigneeException extends DomainException {

    private final Long taskId;

    private TaskHasNoAssigneeException(String message, Long taskId) {
        super(message);
        this.taskId = taskId;
    }

    public static TaskHasNoAssigneeException cannotStart(Long taskId) {
        return new TaskHasNoAssigneeException(
                "Задачу с id=%d нельзя перевести в статус IN_PROGRESS без исполнителя"
                        .formatted(taskId),
                taskId
        );
    }
}
