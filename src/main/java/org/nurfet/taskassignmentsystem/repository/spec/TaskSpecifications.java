package org.nurfet.taskassignmentsystem.repository.spec;

import org.nurfet.taskassignmentsystem.domain.entity.Task;
import org.nurfet.taskassignmentsystem.domain.enums.TaskPriority;
import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;
import org.nurfet.taskassignmentsystem.util.LikeUtils;
import org.springframework.data.jpa.domain.Specification;


import java.util.Locale;

public final class TaskSpecifications {

    private TaskSpecifications() {}

    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Task> hasPriority(TaskPriority priority) {
        return (root, query, cb) -> priority == null
                ? cb.conjunction()
                : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Task> hasAssignee(Long assigneeId) {
        return (root, query, cb) -> assigneeId == null
                ? cb.conjunction()
                : cb.equal(root.get("assignee").get("id"), assigneeId);
    }

    public static Specification<Task> titleContains(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + LikeUtils.escape(q.strip().toLowerCase(Locale.ROOT)) + "%";
            return cb.like(cb.lower(root.get("title")), pattern, '\\');
        };
    }

    public static Specification<Task> byFilter(TaskFilter f) {
        return Specification.allOf(
                hasStatus(f.status()),
                hasPriority(f.priority()),
                hasAssignee(f.assigneeId()),
                titleContains(f.q()));
    }
}
