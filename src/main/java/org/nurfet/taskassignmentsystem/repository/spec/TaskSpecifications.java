package org.nurfet.taskassignmentsystem.repository.spec;

import jakarta.persistence.criteria.Predicate;
import org.nurfet.taskassignmentsystem.domain.entity.Task;
import org.nurfet.taskassignmentsystem.util.LikeUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TaskSpecifications {

    private TaskSpecifications() {}

    public static Specification<Task> byFilter(TaskFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (f.status() != null) {
                predicates.add(cb.equal(root.get("status"), f.status()));
            }
            if (f.priority() != null) {
                predicates.add(cb.equal(root.get("priority"), f.priority()));
            }
            if (f.assigneeId() != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), f.assigneeId()));
            }
            if (f.q() != null && !f.q().isBlank()) {
                String pattern = "%" + LikeUtils.escape(f.q().strip().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.like(cb.lower(root.get("title")), pattern, '\\'));
            }

            // пустой массив даёт условие «всегда истина»
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
