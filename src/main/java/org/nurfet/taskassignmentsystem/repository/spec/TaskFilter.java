package org.nurfet.taskassignmentsystem.repository.spec;

import org.nurfet.taskassignmentsystem.domain.enums.TaskPriority;
import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;

public record TaskFilter(TaskStatus status, TaskPriority priority, Long assigneeId, String q) {
}
