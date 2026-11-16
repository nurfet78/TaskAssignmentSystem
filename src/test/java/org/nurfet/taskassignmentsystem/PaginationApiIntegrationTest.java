package org.nurfet.taskassignmentsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

/**
 * Тесты постраничной выдачи: метаданные страницы, отсутствие потерь и дублей между
 * страницами, ограничение размера, сортировка и фильтры.
 * Ожидает, что в application.yaml задано
 * spring.data.web.pageable.max-page-size: 100 и default-page-size: 20.
 */
class PaginationApiIntegrationTest extends AbstractApiTest {

    // ---------- Хелперы ----------

    private void createTasks(int count) {
        for (int i = 1; i <= count; i++) {
            createTask("Task %02d".formatted(i));
        }
    }

    private void createDevelopers(int count) {
        for (int i = 1; i <= count; i++) {
            createDeveloper("dev%02d@example.com".formatted(i));
        }
    }

    private void createSkills(int count) {
        for (int i = 1; i <= count; i++) {
            createSkill("Skill %02d".formatted(i));
        }
    }

    private long createTaskWithPriority(String title, String priority) {
        return number(bodyOf(post(TASKS, Map.of("title", title, "priority", priority))
                .expectStatus().isCreated()), "id");
    }

    @SuppressWarnings("unchecked")
    private List<Long> idsOf(ResponseSpec spec) {
        Map<String, Object> body = bodyOf(spec.expectStatus().isOk());
        return ((List<Map<String, Object>>) body.get("content")).stream()
                .map(item -> ((Number) item.get("id")).longValue())
                .toList();
    }

    private long createDeveloperNamed(String first, String last, String email) {
        return number(bodyOf(post(DEVELOPERS,
                Map.of("firstName", first, "lastName", last, "email", email))
                .expectStatus().isCreated()), "id");
    }

    // =====================================================================
    // Задачи
    // =====================================================================

    @Test
    void tasks_middlePage_returnsPageMetadata() {
        createTasks(25);

        get(TASKS + "?page=1&size=10")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(10)
                .jsonPath("$.page").isEqualTo(1)
                .jsonPath("$.size").isEqualTo(10)
                .jsonPath("$.totalElements").isEqualTo(25)
                .jsonPath("$.totalPages").isEqualTo(3)
                .jsonPath("$.last").isEqualTo(false);
    }

    @Test
    void tasks_lastPage_hasRemainingItemsAndLastFlag() {
        createTasks(25);

        get(TASKS + "?page=2&size=10")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(5)
                .jsonPath("$.last").isEqualTo(true);
    }

    @Test
    void tasks_pageBeyondRange_returnsEmptyContent() {
        createTasks(5);

        get(TASKS + "?page=5&size=10")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(0)
                .jsonPath("$.totalElements").isEqualTo(5);
    }

    @Test
    void tasks_defaultPageSize_isTwenty() {
        createTasks(25);

        get(TASKS)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.size").isEqualTo(20)
                .jsonPath("$.content.length()").isEqualTo(20);
    }

    @Test
    void tasks_sizeAboveLimit_isCapped() {
        createTasks(3);

        get(TASKS + "?size=1000")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.size").value(Integer.class, size -> assertTrue(size <= 100,
                        "size не ограничен: " + size));
    }

    @Test
    void tasks_allPages_returnEveryTaskExactlyOnce() {
        createTasks(25);

        List<Long> ids = new java.util.ArrayList<>();
        for (int page = 0; page < 3; page++) {
            ids.addAll(idsOf(get(TASKS + "?page={p}&size=10", page)));
        }

        assertEquals(25, ids.size(), "потеряны или лишние элементы");
        assertEquals(25, new HashSet<>(ids).size(), "есть дубли между страницами");
    }

    @Test
    void tasks_sortByTitle_ascAndDesc() {
        createTask("B task");
        createTask("A task");
        createTask("C task");

        get(TASKS + "?sort=title,asc")
                .expectStatus().isOk()
                .expectBody().jsonPath("$.content[0].title").isEqualTo("A task");

        get(TASKS + "?sort=title,desc")
                .expectStatus().isOk()
                .expectBody().jsonPath("$.content[0].title").isEqualTo("C task");
    }

    @Test
    void tasks_unknownSortField_returns400() {
        createTasks(1);

        get(TASKS + "?sort=nope").expectStatus().isBadRequest();
    }

    @Test
    void tasks_sortByRelationField_returns400() {
        createTasks(1);

        get(TASKS + "?sort=assignee.email").expectStatus().isBadRequest();
    }

    @Test
    void tasks_statusFilter_isAppliedToPageAndTotals() {
        long java = createSkill("Java");
        long dev = createDeveloper("dev@example.com", java);
        for (int i = 1; i <= 12; i++) {
            createTask("Free %02d".formatted(i), java);
        }
        for (int i = 1; i <= 3; i++) {
            assign(createTask("Taken %02d".formatted(i), java), dev).expectStatus().isOk();
        }

        get(TASKS + "?status=NEW&size=5")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(5)
                .jsonPath("$.totalElements").isEqualTo(12)
                .jsonPath("$.totalPages").isEqualTo(3);
    }

    @Test
    void tasks_pageIncludesAssigneeAndSkills() {
        long java = createSkill("Java");
        long dev = createDeveloper("dev@example.com", java);
        assign(createTask("Task", java), dev).expectStatus().isOk();

        get(TASKS + "?size=10")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[0].assignee.id").isEqualTo(dev)
                .jsonPath("$.content[0].requiredSkills.length()").isEqualTo(1);
    }

    // =====================================================================
    // Разработчики
    // =====================================================================

    @Test
    void developers_page_returnsMetadata() {
        createDevelopers(25);

        get(DEVELOPERS + "?page=1&size=10")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(10)
                .jsonPath("$.totalElements").isEqualTo(25)
                .jsonPath("$.totalPages").isEqualTo(3);
    }

    @Test
    void developers_allPages_returnEveryDeveloperExactlyOnce() {
        createDevelopers(25);

        List<Long> ids = new java.util.ArrayList<>();
        for (int page = 0; page < 3; page++) {
            ids.addAll(idsOf(get(DEVELOPERS + "?page={p}&size=10", page)));
        }

        assertEquals(25, ids.size());
        assertEquals(25, new HashSet<>(ids).size());
    }

    @Test
    void developers_activeOnly_countsOnlyActive() {
        createDevelopers(10);
        Set<Long> toDeactivate = new HashSet<>(idsOf(get(DEVELOPERS + "?size=3")));
        toDeactivate.forEach(id -> setActive(id, false).expectStatus().isOk());

        get(DEVELOPERS + "?activeOnly=true&size=5")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(7)
                .jsonPath("$.content.length()").isEqualTo(5);
    }

    @Test
    void developers_page_containsSkillsAndTaskCount() {
        long java = createSkill("Java");
        long dev = createDeveloper("dev@example.com", java);
        assign(createTask("Task", java), dev).expectStatus().isOk();

        get(DEVELOPERS + "?size=10")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[0].skills.length()").isEqualTo(1)
                .jsonPath("$.content[0].activeTaskCount").isEqualTo(1);
    }

    @Test
    void developers_unknownSortField_returns400() {
        get(DEVELOPERS + "?sort=nope").expectStatus().isBadRequest();
    }

    // =====================================================================
    // Навыки
    // =====================================================================

    @Test
    void skills_page_returnsMetadataAndSortsByName() {
        createSkills(25);

        get(SKILLS + "?page=0&size=10")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(10)
                .jsonPath("$.totalElements").isEqualTo(25)
                .jsonPath("$.content[0].name").isEqualTo("Skill 01");
    }

    @Test
    void skills_allPages_returnEverySkillExactlyOnce() {
        createSkills(25);

        List<Long> ids = new java.util.ArrayList<>();
        for (int page = 0; page < 3; page++) {
            ids.addAll(idsOf(get(SKILLS + "?page={p}&size=10", page)));
        }

        assertEquals(25, ids.size());
        assertEquals(25, new HashSet<>(ids).size());
    }

    @Test
    void skills_unknownSortField_returns400() {
        get(SKILLS + "?sort=nope").expectStatus().isBadRequest();
    }

    @Test
    void tasks_sortByTitleWithDuplicates_noLossOrRepeat() {
        for (int i = 0; i < 25; i++) {
            createTask("Same title");
        }

        List<Long> ids = new java.util.ArrayList<>();
        for (int page = 0; page < 3; page++) {
            ids.addAll(idsOf(get(TASKS + "?sort=title,asc&page={p}&size=10", page)));
        }

        assertEquals(25, new HashSet<>(ids).size(), "есть дубли или потери");
        assertEquals(ids.stream().sorted().toList(), ids, "внутри равных ключей порядок по id");
    }

    @Test
    void developers_sortByActiveWithDuplicates_noLossOrRepeat() {
        createDevelopers(25);

        List<Long> ids = new java.util.ArrayList<>();
        for (int page = 0; page < 3; page++) {
            ids.addAll(idsOf(get(DEVELOPERS + "?sort=active&page={p}&size=10", page)));
        }

        assertEquals(25, new HashSet<>(ids).size());
    }

    @Test
    void tasks_filterByAssignee_includesDoneTasks() {
        long java = createSkill("Java");
        long dev = createDeveloper("dev@example.com", java);
        long other = createDeveloper("other@example.com", java);
        long active = createTask("Mine active", java);
        long done = createTask("Mine done", java);
        long foreign = createTask("Not mine", java);
        createTask("Nobody", java);

        assign(active, dev).expectStatus().isOk();
        assign(done, dev).expectStatus().isOk();
        patch(TASKS + "/{id}/status",
                Map.of("newStatus", "DONE", "version", taskVersion(done)), done).expectStatus().isOk();
        assign(foreign, other).expectStatus().isOk();

        get(TASKS + "?assigneeId={id}", dev)
                .expectStatus().isOk()
                .expectBody().jsonPath("$.totalElements").isEqualTo(2);

        get(TASKS + "?assigneeId={id}&status=IN_PROGRESS", dev)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(1)
                .jsonPath("$.content[0].title").isEqualTo("Mine active");
    }

    @Test
    void tasks_unknownAssignee_returnsEmptyPage() {
        createTasks(2);

        get(TASKS + "?assigneeId=999")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(0)
                .jsonPath("$.content.length()").isEqualTo(0);
    }

    @Test
    void tasks_filterByPriority() {
        createTaskWithPriority("A", "HIGH");
        createTaskWithPriority("B", "LOW");
        createTaskWithPriority("C", "HIGH");

        get(TASKS + "?priority=HIGH")
                .expectStatus().isOk()
                .expectBody().jsonPath("$.totalElements").isEqualTo(2);
    }

    @Test
    void tasks_invalidPriority_returns400() {
        get(TASKS + "?priority=URGENT").expectStatus().isBadRequest();
    }

    @Test
    void tasks_searchByTitle_ignoresCase() {
        createTask("Настроить CI");
        createTask("Fix login");

        get(TASKS + "?q=настроить").expectStatus().isOk()
                .expectBody().jsonPath("$.totalElements").isEqualTo(1);
        get(TASKS + "?q=LOGIN").expectStatus().isOk()
                .expectBody().jsonPath("$.totalElements").isEqualTo(1);
    }

    @Test
    void tasks_searchByTitle_wildcardsAreLiteral() {
        createTask("100% done");
        createTask("a_c");
        createTask("abc");

        get(TASKS + "?q={q}", "%").expectStatus().isOk()
                .expectBody().jsonPath("$.totalElements").isEqualTo(1);
        get(TASKS + "?q={q}", "_").expectStatus().isOk()
                .expectBody().jsonPath("$.totalElements").isEqualTo(1);
    }

    @Test
    void tasks_blankQuery_isIgnored() {
        createTasks(3);

        get(TASKS + "?q={q}", " ")
                .expectStatus().isOk()
                .expectBody().jsonPath("$.totalElements").isEqualTo(3);
    }

    @Test
    void tasks_combinedFilters_applyAsAnd() {
        createTaskWithPriority("Fix bug", "HIGH");
        createTaskWithPriority("Fix typo", "LOW");
        createTaskWithPriority("Write docs", "HIGH");

        get(TASKS + "?priority=HIGH&q=fix")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(1)
                .jsonPath("$.content[0].title").isEqualTo("Fix bug");
    }

    @Test
    void tasks_tooLongQuery_returns400() {
        get(TASKS + "?q={q}", "x".repeat(201)).expectStatus().isBadRequest();
    }

    @Test
    void developers_search_byNameFullNameAndEmail() {
        long alexey = createDeveloperNamed("Алексей", "Иванов", "a.ivanov@example.com");
        createDeveloperNamed("Мария", "Петрова", "maria@example.com");

        for (String q : new String[]{"алексей", "ИВАНОВ", "алексей иванов", "a.ivanov@"}) {
            get(DEVELOPERS + "?q={q}", q)
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.totalElements").isEqualTo(1)
                    .jsonPath("$.content[0].id").isEqualTo(alexey);
        }
    }

    @Test
    void developers_search_withActiveOnly() {
        createDeveloperNamed("Иван", "Один", "one@example.com");
        long two = createDeveloperNamed("Иван", "Два", "two@example.com");
        setActive(two, false).expectStatus().isOk();

        get(DEVELOPERS + "?q=иван").expectStatus().isOk()
                .expectBody().jsonPath("$.totalElements").isEqualTo(2);
        get(DEVELOPERS + "?q=иван&activeOnly=true").expectStatus().isOk()
                .expectBody().jsonPath("$.totalElements").isEqualTo(1);
    }

    @Test
    void developers_search_wildcardIsLiteral() {
        createDeveloperNamed("Иван", "Один", "one@example.com");
        createDeveloperNamed("100%", "Скидка", "sale@example.com");

        get(DEVELOPERS + "?q={q}", "%").expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(1)
                .jsonPath("$.content[0].email").isEqualTo("sale@example.com");
    }

    @Test
    void developers_search_noMatch_returnsEmptyPage() {
        createDevelopers(3);

        get(DEVELOPERS + "?q=nobody").expectStatus().isOk()
                .expectBody().jsonPath("$.totalElements").isEqualTo(0);
    }

    @Test
    void skills_search_ignoresCase() {
        createSkill("Java");
        createSkill("JavaScript");
        createSkill("Python");

        get(SKILLS + "?q=JAVA").expectStatus().isOk()
                .expectBody().jsonPath("$.totalElements").isEqualTo(2);
        get(SKILLS + "?q=thon").expectStatus().isOk()
                .expectBody().jsonPath("$.totalElements").isEqualTo(1);
    }

    @Test
    void skills_search_wildcardsAreLiteral() {
        createSkill("C++");
        createSkill("C#");
        createSkill("100%");

        get(SKILLS + "?q={q}", "%").expectStatus().isOk()
                .expectBody().jsonPath("$.totalElements").isEqualTo(1);
    }

    @Test
    void skills_search_blankQueryReturnsAll() {
        createSkills(3);

        get(SKILLS + "?q={q}", " ").expectStatus().isOk()
                .expectBody().jsonPath("$.totalElements").isEqualTo(3);
    }
}
