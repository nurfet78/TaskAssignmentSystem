package org.nurfet.taskassignmentsystem;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.nurfet.taskassignmentsystem.util.Validate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void tiebreaker_addsIdAfterExistingSort() {
        Pageable p = Validate.withIdTiebreaker(PageRequest.of(1, 10,
                Sort.by(Sort.Direction.DESC, "title")));

        var orders = p.getSort().toList();
        assertEquals(2, orders.size());
        assertEquals("title", orders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());
        assertEquals("id", orders.get(1).getProperty());
        assertEquals(1, p.getPageNumber());
        assertEquals(10, p.getPageSize());
    }

    @Test
    void tiebreaker_unsorted_getsIdOnly() {
        Pageable p = Validate.withIdTiebreaker(PageRequest.of(0, 20));

        assertEquals(1, p.getSort().toList().size());
        assertEquals("id", p.getSort().toList().getFirst().getProperty());
    }

    @Test
    void tiebreaker_idAlreadyPresent_returnsSameInstance() {
        Pageable original = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        assertSame(original, Validate.withIdTiebreaker(original));
    }

    @Test
    void tiebreaker_unpaged_returnsSameInstance() {
        assertSame(Pageable.unpaged(), Validate.withIdTiebreaker(Pageable.unpaged()));
    }
}
