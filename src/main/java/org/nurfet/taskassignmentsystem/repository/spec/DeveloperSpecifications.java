package org.nurfet.taskassignmentsystem.repository.spec;

import jakarta.persistence.criteria.Expression;
import org.nurfet.taskassignmentsystem.domain.entity.Developer;
import org.nurfet.taskassignmentsystem.util.LikeUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class DeveloperSpecifications {

    private DeveloperSpecifications() {}

    public static Specification<Developer> isActive(boolean activeOnly) {
        return (root, query, cb) -> activeOnly
                ? cb.isTrue(root.get("active"))
                : cb.conjunction();
    }

    public static Specification<Developer> matches(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return cb.conjunction();
            }

            String pattern = "%" + LikeUtils.escape(q.strip().toLowerCase(Locale.ROOT)) + "%";

            Expression<String> fullName = cb.lower(
                    cb.concat(cb.concat(root.<String>get("firstName"), " "),
                            root.<String>get("lastName")));

            return cb.or(
                    cb.like(fullName, pattern, '\\'),
                    cb.like(cb.lower(root.<String>get("email")), pattern, '\\'));
        };
    }

    public static Specification<Developer> byFilter(boolean activeOnly, String q) {
        return Specification.allOf(
                isActive(activeOnly),
                matches(q));
    }
}
