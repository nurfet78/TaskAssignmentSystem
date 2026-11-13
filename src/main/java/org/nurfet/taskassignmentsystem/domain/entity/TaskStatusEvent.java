package org.nurfet.taskassignmentsystem.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;
import org.nurfet.taskassignmentsystem.util.Validate;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "task_status_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class TaskStatusEvent extends BaseEntity {

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private TaskStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "new_status", nullable = false, length = 20)
    private TaskStatus newStatus;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "assignee_email")
    private String assigneeEmail;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "task_version")
    private Long taskVersion;

    @Builder
    private TaskStatusEvent(Long taskId, TaskStatus previousStatus, TaskStatus newStatus,
                            Long assigneeId, String assigneeEmail, String comment, Long taskVersion) {

        this.taskId = Validate.notNull(taskId, "taskId");
        this.previousStatus = previousStatus;
        this.newStatus = Validate.notNull(newStatus, "newStatus");
        this.assigneeId = assigneeId;
        this.assigneeEmail = assigneeEmail;
        this.occurredAt = Instant.now();
        this.comment = comment;
        this.taskVersion = taskVersion;
    }

    public static TaskStatusEvent fromTask(Task task, TaskStatus previousStatus, String comment) {

        Developer assignee = task.getAssignee();

        return builder()
                .taskId(task.getId())
                .previousStatus(previousStatus)
                .newStatus(task.getStatus())
                .assigneeId(assignee != null ? assignee.getId() : null)
                .assigneeEmail(assignee != null ? assignee.getEmail() : null)
                .comment(comment)
                .taskVersion(task.getVersion())
                .build();
    }

    public static TaskStatusEvent taskCreated(Task task) {

        return builder()
                .taskId(task.getId())
                .previousStatus(null)
                .newStatus(task.getStatus())
                .comment("Задача создана")
                .taskVersion(task.getVersion())
                .build();
    }

    private static String buildComment(Developer assignee, Developer previousAssignee, String comment) {
        if (comment != null) {
            return comment;
        }

        if (assignee == null && previousAssignee == null) {
            return "Исполнитель отсутствует";
        }

        if (previousAssignee == null) {
            return "Исполнитель назначен " + assignee.getEmail();
        }

        if (assignee == null) {
            return "Исполнитель снят " + previousAssignee.getEmail();
        }

        if (assignee.equals(previousAssignee)) {
            return "Исполнитель не изменился";
        }

        return "Произошла смена исполнителя %s -> %s".formatted(previousAssignee, assignee);
    }

    public static TaskStatusEvent assigneeChanged(Task task, TaskStatus previousStatus,
                                                  Developer previousAssignee, String comment) {
        Developer assignee = task.getAssignee();

        return builder()
                .taskId(task.getId())
                .previousStatus(previousStatus)
                .newStatus(task.getStatus())
                .assigneeId(assignee != null ? assignee.getId() : null)
                .assigneeEmail(assignee != null ? assignee.getEmail() : null)
                .comment(buildComment(assignee, previousAssignee, comment))
                .taskVersion(task.getVersion())
                .build();
    }

    public static TaskStatusEvent create(Long taskId, TaskStatus previousStatus, TaskStatus newStatus,
                                         Long assigneeId, String assigneeEmail, String comment, Long taskVersion) {

        return builder()
                .taskId(taskId)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .assigneeId(assigneeId)
                .assigneeEmail(assigneeEmail)
                .comment(comment)
                .taskVersion(taskVersion)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskStatusEvent that)) return false;
        if (getId() == null || that.getId() == null) return false;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "TaskStatusEvent{" +
                "taskId=" + taskId +
                ", previousStatus=" + previousStatus +
                ", newStatus=" + newStatus +
                ", occurredAt=" + occurredAt +
                '}';
    }
}