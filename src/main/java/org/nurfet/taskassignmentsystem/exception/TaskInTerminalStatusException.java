package org.nurfet.taskassignmentsystem.exception;

import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;

import lombok.Getter;

@Getter
public class TaskInTerminalStatusException extends DomainException {

    private final Long taskId;
    private final TaskStatus status;

    public TaskInTerminalStatusException(Long taskId, TaskStatus status) {
        super("Задача %d находится в терминальном статусе %s".formatted(taskId, status));

        this.taskId = taskId;
        this.status = status;
    }
}