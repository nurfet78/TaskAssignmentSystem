package org.nurfet.taskassignmentsystem;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.util.List;
import java.util.Map;

/**
 * Общая база для HTTP-тестов: клиент, очистка БД, хелперы создания данных
 * и запуск вызовов в нескольких потоках одновременно.
 *
 * Тела запросов собираются из Map по ИМЕНАМ полей, а не через конструкторы DTO:
 * так порядок параметров в record не влияет на тесты.
 */
public abstract class AbstractApiTest extends AbstractIntegrationTest {

    protected static final String SKILLS = "/api/v1/skills";
    protected static final String DEVELOPERS = "/api/v1/developers";
    protected static final String TASKS = "/api/v1/tasks";

    @Autowired
    private WebTestClient webTestClient;

    /**
     * Клиент Spring Boot с увеличенным таймаутом: по умолчанию WebTestClient
     * ждёт ответ всего 5 секунд. HTTP-клиент под ним выбирается автоматически
     * по classpath (Reactor Netty, если он есть, иначе клиент из JDK).
     *
     * ЕСЛИ ТЕСТЫ ЗАВИСАЮТ на запросах с телом (POST, PATCH, PUT) и падают с
     * "Timeout on blocking read for ... NANOSECONDS", а в дампе потоков Tomcat стоит
     * на чтении тела запроса (NioSocketWrapper.fillReadBuffer), проверьте, что в pom.xml
     * есть тестовая зависимость io.projectreactor.netty:reactor-netty-http.
     * Без неё WebTestClient использует клиент из JDK, с которым такие запросы зависали.
     */
    protected WebTestClient client;

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void setUpClient() {
        client = webTestClient.mutate()
                .responseTimeout(Duration.ofSeconds(30))
                .build();
    }

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("""
                truncate task_status_events, task_skills, tasks,
                         developer_skills, developers, skills
                restart identity cascade
                """);
    }

    // ---------- HTTP ----------

    protected ResponseSpec get(String uri, Object... vars) {
        return client.get().uri(uri, vars).exchange();
    }

    protected ResponseSpec post(String uri, Object body, Object... vars) {
        return client.post().uri(uri, vars).contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
    }

    protected ResponseSpec postEmpty(String uri, Object... vars) {
        return client.post().uri(uri, vars).exchange();
    }

    protected ResponseSpec put(String uri, Object body, Object... vars) {
        return client.put().uri(uri, vars).contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
    }

    protected ResponseSpec patch(String uri, Object body, Object... vars) {
        return client.patch().uri(uri, vars).contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
    }

    protected ResponseSpec delete(String uri, Object... vars) {
        return client.delete().uri(uri, vars).exchange();
    }

    protected Map<String, Object> bodyOf(ResponseSpec spec) {
        return spec.expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult().getResponseBody();
    }

    /** Код ответа и поле type из тела ошибки (например, "/error/duplicate"). */
    protected record ApiResult(int status, String type) {}

    protected ApiResult result(ResponseSpec spec) {
        var exchange = spec.expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult();
        Map<String, Object> body = exchange.getResponseBody();
        String type = body == null ? null : (String) body.get("type");

        return new ApiResult(exchange.getStatus().value(), type);
    }

    protected int status(ResponseSpec spec) {
        return spec.expectBody().returnResult().getStatus().value();
    }

    protected static long number(Map<String, Object> body, String field) {
        return ((Number) body.get(field)).longValue();
    }

    // ---------- Создание данных через API ----------

    protected long createSkill(String name) {
        return number(bodyOf(post(SKILLS, Map.of("name", name)).expectStatus().isCreated()), "id");
    }

    protected long createDeveloper(String email, Long... skillIds) {
        Map<String, Object> body = Map.of(
                "firstName", "Ivan",
                "lastName", "Petrov",
                "email", email,
                "skillIds", List.of(skillIds));
        return number(bodyOf(post(DEVELOPERS, body).expectStatus().isCreated()), "id");
    }

    protected long createTask(String title, Long... skillIds) {
        Map<String, Object> body = Map.of(
                "title", title,
                "priority", "MEDIUM",
                "skillIds", List.of(skillIds));
        return number(bodyOf(post(TASKS, body).expectStatus().isCreated()), "id");
    }

    protected long taskVersion(long taskId) {
        return number(bodyOf(get(TASKS + "/{id}", taskId).expectStatus().isOk()), "version");
    }

    protected long developerVersion(long developerId) {
        return number(bodyOf(get(DEVELOPERS + "/{id}", developerId).expectStatus().isOk()), "version");
    }

    protected ResponseSpec assign(long taskId, long developerId) {
        return post(TASKS + "/{id}/assign",
                Map.of("developerId", developerId, "version", taskVersion(taskId)), taskId);
    }

    protected ResponseSpec setActive(long developerId, boolean active) {
        return patch(DEVELOPERS + "/{id}",
                Map.of("version", developerVersion(developerId), "active", active), developerId);
    }

    // ---------- Параллельный запуск ----------

    /**
     * Запускает все вызовы одновременно (все потоки ждут общий сигнал старта)
     * и возвращает результаты в том же порядке.
     */
    protected <T> List<T> runConcurrently(List<Callable<T>> calls) throws Exception {
        int n = calls.size();
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> call : calls) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return call.call();
                }));
            }
            ready.await();
            start.countDown();

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    protected static long count(List<Integer> statuses, int code) {
        return statuses.stream().filter(s -> s == code).count();
    }

    protected static long countSuccessful(List<Integer> statuses) {
        return statuses.stream().filter(s -> s >= 200 && s < 300).count();
    }
}