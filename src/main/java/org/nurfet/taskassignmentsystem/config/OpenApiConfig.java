package org.nurfet.taskassignmentsystem.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Task Assignment System API")
                        .version("1.0.0")
                        .description("""
                                REST API для системы автоматического распределения задач между разработчиками.

                                ## Основные возможности
                                - Управление задачами
                                - Управление разработчиками
                                - Управление навыками
                                - Автоматическое и ручное назначение задач
                                - Просмотр истории изменения статусов

                                ## Алгоритм автоматического назначения
                                1. Поиск активных разработчиков, имеющих хотя бы один требуемый навык.
                                2. Определение количества совпадающих навыков и текущей загрузки каждого кандидата.
                                3. Приоритет разработчикам с полным совпадением навыков.
                                4. Если полного совпадения нет — выбор кандидата с наибольшим количеством совпадающих навыков.
                                5. При равном количестве совпадений предпочтение отдаётся разработчику с минимальной загрузкой.

                                ## Статусы задач
                                - **NEW** — новая задача
                                - **IN_PROGRESS** — задача выполняется
                                - **DONE** — задача завершена
                                - **CANCELLED** — задача отменена

                                ## Бизнес-правила
                                - Задачи в статусах DONE и CANCELLED являются терминальными.
                                - Разработчик должен обладать хотя бы одним требуемым навыком.
                                - При назначении задачи статус NEW автоматически изменяется на IN_PROGRESS.
                                """)
                        .contact(new Contact()
                                .name("Nuri")
                                .email("your.email@example.com"))
                        .license(new License()
                                .name("MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project documentation")
                        .url("https://github.com/username/task-assignment-system"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local environment")
                ))
                .tags(List.of(
                        new Tag()
                                .name("Tasks")
                                .description("Управление задачами и их назначением"),
                        new Tag()
                                .name("Developers")
                                .description("Управление разработчиками"),
                        new Tag()
                                .name("Skills")
                                .description("Управление навыками"),
                        new Tag()
                                .name("Reports")
                                .description("Отчёты и статистика")
                ));
    }
}
