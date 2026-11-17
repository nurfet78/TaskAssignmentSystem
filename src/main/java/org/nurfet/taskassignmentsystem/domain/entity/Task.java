package org.nurfet.taskassignmentsystem.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.nurfet.taskassignmentsystem.domain.enums.TaskPriority;
import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;
import org.nurfet.taskassignmentsystem.exception.*;
import org.nurfet.taskassignmentsystem.util.Validate;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "tasks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Task extends AuditableEntity {

    @Version
    private Long version;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'NEW'")
    @NotNull
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'MEDIUM'")
    @NotNull
    @Column(name = "priority", nullable = false, length = 20)
    private TaskPriority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Developer assignee;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToMany
    @JoinTable(
            name = "task_skills",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> requiredSkills = new HashSet<>();

    public Task(String title, String description, TaskPriority priority) {
        this.title = Validate.notBlank(title, "title", 200);
        this.description = description;
        this.priority = Objects.requireNonNullElse(priority, TaskPriority.MEDIUM);
        this.status = TaskStatus.NEW;
    }

    public void update(String title, String description, TaskPriority priority) {
        if (title != null) {
            this.title = Validate.notBlank(title, "title", 200);
        }

        if (description != null) {
            this.description = description;
        }

        if (priority != null) {
            this.priority = priority;
        }
    }

    public void replaceRequiredSkills(Set<Skill> skills) {
        requiredSkills.clear();
        skills.forEach(this::addSkill);
    }

    public boolean isAssigned() {
        return assignee != null;
    }

    public void addSkill(Skill skill) {
        requiredSkills.add(skill);
    }

    public void removeSkill(Skill skill) {
        requiredSkills.remove(skill);
    }

    public void assignTo(Developer developer) {

        if (status.isTerminal()) {
            throw new TaskInTerminalStatusException(getId(), status);
        }

        if (!developer.isActive()) {
            throw TaskAssignmentFailedException.developerInactive(getId(), developer.getEmail());
        }

        if (!requiredSkills.isEmpty() && !developer.hasAnySkill(requiredSkills)) {
            throw new MissingRequiredSkillsException(getId(), developer.getEmail());
        }

        assignee = developer;
        assignedAt = Instant.now();

        if (status == TaskStatus.NEW) {
            status = TaskStatus.IN_PROGRESS;
        }
    }

    public void unassign() {
        if (status.isTerminal()) {
            throw new TaskInTerminalStatusException(getId(), status);
        }

        assignee = null;
        assignedAt = null;

        if (status == TaskStatus.IN_PROGRESS) {
            status = TaskStatus.NEW;
        }
    }

    public void transitionTo(TaskStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw InvalidStatusTransitionException.of(status, newStatus);
        }

        if (newStatus == TaskStatus.IN_PROGRESS && assignee == null) {
            throw TaskHasNoAssigneeException.cannotStart(getId());
        }

        status = newStatus;
        if (newStatus.isTerminal()) {
            completedAt = Instant.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task task)) return false;
        if (getId() == null || task.getId() == null) return false;
        return Objects.equals(getId(), task.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + getId() +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", priority=" + priority +
                ", assignee=" + (assignee != null ? assignee : "unassigned") +
                '}';
    }
}