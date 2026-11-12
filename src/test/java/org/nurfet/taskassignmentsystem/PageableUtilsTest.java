package org.nurfet.taskassignmentsystem;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.nurfet.taskassignmentsystem.util.Validate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Юнит-тест */
class PageableUtilsTest {

    private static final Set<String> ALLOWED = Set.of("id", "title", "createdAt");

    @Test
    void allowedField_passes() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        assertDoesNotThrow(() -> Validate.validateSort(pageable, ALLOWED));
    }

    @Test
    void unsorted_passes() {
        assertDoesNotThrow(() -> Validate.validateSort(PageRequest.of(0, 20), ALLOWED));
        assertDoesNotThrow(() -> Validate.validateSort(Pageable.unpaged(), ALLOWED));
    }

    @Test
    void unknownField_throws_andListsAllowedFields() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("nope"));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Validate.validateSort(pageable, ALLOWED));

        assertTrue(e.getMessage().contains("nope"));
        assertTrue(e.getMessage().contains("createdAt, id, title"));
    }

    @Test
    void oneUnknownAmongSeveral_throws() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("title", "assignee.email"));

        assertThrows(IllegalArgumentException.class,
                () -> Validate.validateSort(pageable, ALLOWED));
    }
}
