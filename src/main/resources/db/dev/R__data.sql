-- =============================================================================
-- SEED DATA (профиль dev, db/dev)
--
-- Порядок выполнения:
--   1. Навыки
--   2. Разработчики
--   3. Навыки разработчиков
--   4. Задачи (с явным created_at)
--   5. Исполнители и статусы (UPDATE tasks), до вставки событий
--   6. Требуемые навыки задач
--   7. История событий (task_status_events), после всех UPDATE tasks
--
-- Инварианты, которые соблюдает этот скрипт (так же, как приложение):
--   - NEW  -> исполнителя нет;
--   - IN_PROGRESS -> исполнитель есть и у него есть хотя бы один нужный навык;
--   - DONE -> исполнитель сохраняется (кто сделал);
--   - CANCELLED -> здесь без исполнителя;
--   - у неактивного разработчика нет активных задач;
--   - email только в нижнем регистре;
--   - tasks.version равна максимальной task_version из журнала событий задачи.
--
-- Везде используется clock_timestamp(), а не now(): now() фиксируется на
-- начало транзакции, и из-за этого completed_at мог оказаться раньше created_at.
-- created_at задан явно, чтобы точка отсчёта не "плыла".
-- =============================================================================


-- =============================================================================
-- 1. НАВЫКИ
-- =============================================================================
INSERT INTO skills (name, description) VALUES
    ('Java',           'Язык программирования Java'),
    ('Spring',         'Spring Framework / Spring Boot'),
    ('PostgreSQL',     'Реляционная СУБД PostgreSQL'),
    ('Docker',         'Контейнеризация Docker'),
    ('React',          'Библиотека React для UI'),
    ('TypeScript',     'Типизированный JavaScript'),
    ('Python',         'Язык программирования Python'),
    ('ML',             'Machine Learning'),
    ('AWS',            'Amazon Web Services'),
    ('Kubernetes',     'Оркестрация контейнеров Kubernetes'),
    ('Redis',          'In-memory key-value storage'),
    ('JUnit',          'Unit and integration testing'),
    ('Testcontainers', 'Container-based integration testing');


-- =============================================================================
-- 2. РАЗРАБОТЧИКИ
-- =============================================================================
INSERT INTO developers (first_name, last_name, email, active) VALUES
     ('Алексей', 'Иванов',   'alex.ivanov@example.com',     true),
     ('Мария',   'Петрова',  'maria.petrova@example.com',   true),
     ('Дмитрий', 'Сидоров',  'dmitry.sidorov@example.com',  true),
     ('Елена',   'Козлова',  'elena.kozlova@example.com',   true),
     ('Сергей',  'Николаев', 'sergey.nikolaev@example.com', false);


-- =============================================================================
-- 3. НАВЫКИ РАЗРАБОТЧИКОВ
-- =============================================================================

-- Алексей: Java, Spring, PostgreSQL, Docker
INSERT INTO developer_skills (developer_id, skill_id)
SELECT d.id, s.id
FROM developers d, skills s
WHERE d.email = 'alex.ivanov@example.com'
  AND s.name IN ('Java', 'Spring', 'PostgreSQL', 'Docker');

-- Мария: Java, Spring, React, TypeScript
INSERT INTO developer_skills (developer_id, skill_id)
SELECT d.id, s.id
FROM developers d, skills s
WHERE d.email = 'maria.petrova@example.com'
  AND s.name IN ('Java', 'Spring', 'React', 'TypeScript');

-- Дмитрий: Python, ML, Docker, AWS
INSERT INTO developer_skills (developer_id, skill_id)
SELECT d.id, s.id
FROM developers d, skills s
WHERE d.email = 'dmitry.sidorov@example.com'
  AND s.name IN ('Python', 'ML', 'Docker', 'AWS');

-- Елена: Docker, Kubernetes, AWS
INSERT INTO developer_skills (developer_id, skill_id)
SELECT d.id, s.id
FROM developers d, skills s
WHERE d.email = 'elena.kozlova@example.com'
  AND s.name IN ('Docker', 'Kubernetes', 'AWS');

-- Сергей: Java, Spring (неактивен, задач нет)
INSERT INTO developer_skills (developer_id, skill_id)
SELECT d.id, s.id
FROM developers d, skills s
WHERE d.email = 'sergey.nikolaev@example.com'
  AND s.name IN ('Java', 'Spring');


-- =============================================================================
-- 4. ЗАДАЧИ
-- Все создаются как NEW без исполнителя, created_at задан явно.
-- Статусы и исполнители проставляются на шаге 5.
-- =============================================================================
INSERT INTO tasks (title, description, status, priority, created_at) VALUES
    ('Настроить CI/CD pipeline',
    'Настроить GitHub Actions: сборка, тесты, деплой в Docker',
    'NEW', 'HIGH', clock_timestamp() - interval '4 days'),

    ('Разработать REST API для авторизации',
    'JWT-авторизация на Spring Security',
    'NEW', 'CRITICAL', clock_timestamp() - interval '3 days'),

    ('Обучить модель классификации',
    'Обучить ML-модель на датасете пользователей',
    'NEW', 'MEDIUM', clock_timestamp() - interval '2 days'),

    ('Настроить Kubernetes кластер',
    'Развернуть кластер в AWS, настроить ingress',
    'NEW', 'HIGH', clock_timestamp() - interval '5 days'),

    ('Разработать UI компонент таблицы',
    'React-компонент с сортировкой и пагинацией на TypeScript',
    'NEW', 'MEDIUM', clock_timestamp() - interval '6 days'),

    ('Оптимизировать SQL запросы',
    'Найти медленные запросы через pg_stat_statements, добавить индексы',
    'NEW', 'LOW', clock_timestamp() - interval '2 days'),

    ('Написать документацию API',
    'OpenAPI / Swagger для всех эндпоинтов',
    'NEW', 'LOW', clock_timestamp() - interval '4 days'),

    ('Миграция базы данных на PostgreSQL',
    'Перенести данные с MySQL, написать Flyway-миграции',
    'NEW', 'CRITICAL', clock_timestamp() - interval '1 hour');

-- Задачи, для которых нет подходящего активного разработчика
-- (удобно проверять ошибку автоназначения)
INSERT INTO tasks (title, description, status, priority, created_at) VALUES
    ('Внедрить кэширование сессий',
    'Использовать Redis для хранения сессий пользователей',
    'NEW', 'MEDIUM', clock_timestamp() - interval '1 day'),

    ('Покрыть сервис интеграционными тестами',
    'JUnit + Testcontainers для тестирования взаимодействия с БД',
    'NEW', 'LOW', clock_timestamp() - interval '1 day');

-- Завершённые MEDIUM-задачи (для статистики по времени выполнения)
INSERT INTO tasks (title, description, status, priority, created_at) VALUES
   ('Настроить мониторинг и алерты',
   'Prometheus + Grafana, алерты в Slack при деградации сервисов',
   'NEW', 'MEDIUM', clock_timestamp() - interval '10 days'),

   ('Добавить пагинацию в API каталога',
   'Cursor-based пагинация для эндпоинта списка товаров',
   'NEW', 'MEDIUM', clock_timestamp() - interval '3 days');


-- =============================================================================
-- 5. ИСПОЛНИТЕЛИ, СТАТУСЫ И ВЕРСИИ
-- Выполняется ДО вставки событий: события читают assignee_id из tasks.
-- version совпадает с максимальной task_version в журнале (шаг 7).
-- Задачи 6, 8, 9, 10 остаются NEW без исполнителя и с version = 0.
-- =============================================================================

-- Задача 1 (CI/CD) -> Алексей, IN_PROGRESS
UPDATE tasks
SET assignee_id = (SELECT id FROM developers WHERE email = 'alex.ivanov@example.com'),
    assigned_at = clock_timestamp() - interval '3 days',
    status      = 'IN_PROGRESS',
    version     = 1,
    updated_at  = clock_timestamp() - interval '3 days'
WHERE title = 'Настроить CI/CD pipeline';

-- Задача 2 (REST API) -> Алексей, IN_PROGRESS
UPDATE tasks
SET assignee_id = (SELECT id FROM developers WHERE email = 'alex.ivanov@example.com'),
    assigned_at = clock_timestamp() - interval '2 days',
    status      = 'IN_PROGRESS',
    version     = 1,
    updated_at  = clock_timestamp() - interval '2 days'
WHERE title = 'Разработать REST API для авторизации';

-- Задача 3 (ML-модель) -> Дмитрий, IN_PROGRESS
UPDATE tasks
SET assignee_id = (SELECT id FROM developers WHERE email = 'dmitry.sidorov@example.com'),
    assigned_at = clock_timestamp() - interval '1 day',
    status      = 'IN_PROGRESS',
    version     = 1,
    updated_at  = clock_timestamp() - interval '1 day'
WHERE title = 'Обучить модель классификации';

-- Задача 4 (Kubernetes) -> Елена, IN_PROGRESS
UPDATE tasks
SET assignee_id = (SELECT id FROM developers WHERE email = 'elena.kozlova@example.com'),
    assigned_at = clock_timestamp() - interval '4 days',
    status      = 'IN_PROGRESS',
    version     = 1,
    updated_at  = clock_timestamp() - interval '4 days'
WHERE title = 'Настроить Kubernetes кластер';

-- Задача 5 (UI компонент) -> Мария, DONE
-- created_at = -6d, completed_at = -1d (позже created_at).
-- version = 4: четыре изменения в журнале (см. шаг 7).
UPDATE tasks
SET assignee_id  = (SELECT id FROM developers WHERE email = 'maria.petrova@example.com'),
    assigned_at  = clock_timestamp() - interval '2 days',
    status       = 'DONE',
    completed_at = clock_timestamp() - interval '1 day',
    version      = 4,
    updated_at   = clock_timestamp() - interval '1 day'
WHERE title = 'Разработать UI компонент таблицы';

-- Задача 7 (Документация API) -> CANCELLED, без исполнителя
UPDATE tasks
SET status     = 'CANCELLED',
    version    = 1,
    updated_at = clock_timestamp() - interval '3 days'
WHERE title = 'Написать документацию API';

-- Задача 11 (Мониторинг и алерты) -> Елена, DONE
-- created_at = -10d, completed_at = -7d
UPDATE tasks
SET assignee_id  = (SELECT id FROM developers WHERE email = 'elena.kozlova@example.com'),
    assigned_at  = clock_timestamp() - interval '9 days',
    status       = 'DONE',
    completed_at = clock_timestamp() - interval '7 days',
    version      = 2,
    updated_at   = clock_timestamp() - interval '7 days'
WHERE title = 'Настроить мониторинг и алерты';

-- Задача 12 (Пагинация API каталога) -> Алексей, DONE
-- created_at = -3d, completed_at = -1d
UPDATE tasks
SET assignee_id  = (SELECT id FROM developers WHERE email = 'alex.ivanov@example.com'),
    assigned_at  = clock_timestamp() - interval '2 days',
    status       = 'DONE',
    completed_at = clock_timestamp() - interval '1 day',
    version      = 2,
    updated_at   = clock_timestamp() - interval '1 day'
WHERE title = 'Добавить пагинацию в API каталога';


-- =============================================================================
-- 6. ТРЕБУЕМЫЕ НАВЫКИ ЗАДАЧ
-- Каждый исполнитель имеет хотя бы один из навыков своей задачи.
-- =============================================================================

-- Задача 1: Docker, Spring
INSERT INTO task_skills (task_id, skill_id)
SELECT t.id, s.id
FROM tasks t, skills s
WHERE t.title = 'Настроить CI/CD pipeline'
  AND s.name IN ('Docker', 'Spring');

-- Задача 2: Java, Spring
INSERT INTO task_skills (task_id, skill_id)
SELECT t.id, s.id
FROM tasks t, skills s
WHERE t.title = 'Разработать REST API для авторизации'
  AND s.name IN ('Java', 'Spring');

-- Задача 3: Python, ML
INSERT INTO task_skills (task_id, skill_id)
SELECT t.id, s.id
FROM tasks t, skills s
WHERE t.title = 'Обучить модель классификации'
  AND s.name IN ('Python', 'ML');

-- Задача 4: Kubernetes, Docker, AWS
INSERT INTO task_skills (task_id, skill_id)
SELECT t.id, s.id
FROM tasks t, skills s
WHERE t.title = 'Настроить Kubernetes кластер'
  AND s.name IN ('Kubernetes', 'Docker', 'AWS');

-- Задача 5: React, TypeScript
INSERT INTO task_skills (task_id, skill_id)
SELECT t.id, s.id
FROM tasks t, skills s
WHERE t.title = 'Разработать UI компонент таблицы'
  AND s.name IN ('React', 'TypeScript');

-- Задача 6: PostgreSQL (NEW, ждёт автоназначения: подойдёт Алексей)
INSERT INTO task_skills (task_id, skill_id)
SELECT t.id, s.id
FROM tasks t, skills s
WHERE t.title = 'Оптимизировать SQL запросы'
  AND s.name IN ('PostgreSQL');

-- Задача 8: PostgreSQL, Docker (NEW, ждёт автоназначения: полное совпадение у Алексея)
INSERT INTO task_skills (task_id, skill_id)
SELECT t.id, s.id
FROM tasks t, skills s
WHERE t.title = 'Миграция базы данных на PostgreSQL'
  AND s.name IN ('PostgreSQL', 'Docker');

-- Задача 9: Redis (нет подходящих разработчиков)
INSERT INTO task_skills (task_id, skill_id)
SELECT t.id, s.id
FROM tasks t, skills s
WHERE t.title = 'Внедрить кэширование сессий'
  AND s.name IN ('Redis');

-- Задача 10: JUnit, Testcontainers (нет подходящих разработчиков)
INSERT INTO task_skills (task_id, skill_id)
SELECT t.id, s.id
FROM tasks t, skills s
WHERE t.title = 'Покрыть сервис интеграционными тестами'
  AND s.name IN ('JUnit', 'Testcontainers');

-- Задача 11: AWS, Docker
INSERT INTO task_skills (task_id, skill_id)
SELECT t.id, s.id
FROM tasks t, skills s
WHERE t.title = 'Настроить мониторинг и алерты'
  AND s.name IN ('AWS', 'Docker');

-- Задача 12: Java, PostgreSQL
INSERT INTO task_skills (task_id, skill_id)
SELECT t.id, s.id
FROM tasks t, skills s
WHERE t.title = 'Добавить пагинацию в API каталога'
  AND s.name IN ('Java', 'PostgreSQL');


-- =============================================================================
-- 7. ИСТОРИЯ СОБЫТИЙ (task_status_events)
--
-- task_version:
--   0 - создание задачи (NULL -> NEW)
--   1, 2, ... - последовательные изменения
-- Максимальная task_version каждой задачи равна tasks.version (шаг 5).
-- occurred_at события создания берётся из created_at задачи.
-- =============================================================================

-- Создание: NULL -> NEW для всех задач (version = 0)
INSERT INTO task_status_events
(task_id, previous_status, new_status, assignee_id, assignee_email, task_version, comment, occurred_at)
SELECT
    t.id, NULL, 'NEW', NULL, NULL, 0, 'Задача создана', t.created_at
FROM tasks t;

-- Задача 1 (CI/CD): NEW -> IN_PROGRESS (version = 1)
INSERT INTO task_status_events
(task_id, previous_status, new_status, assignee_id, assignee_email, task_version, comment, occurred_at)
SELECT
    t.id, 'NEW', 'IN_PROGRESS', d.id, d.email, 1, 'Взята в работу', clock_timestamp() - interval '3 days'
FROM tasks t
    JOIN developers d ON d.id = t.assignee_id
WHERE t.title = 'Настроить CI/CD pipeline';

-- Задача 2 (REST API): NEW -> IN_PROGRESS (version = 1)
INSERT INTO task_status_events
(task_id, previous_status, new_status, assignee_id, assignee_email, task_version, comment, occurred_at)
SELECT
    t.id, 'NEW', 'IN_PROGRESS', d.id, d.email, 1, 'Взята в работу', clock_timestamp() - interval '2 days'
FROM tasks t
    JOIN developers d ON d.id = t.assignee_id
WHERE t.title = 'Разработать REST API для авторизации';

-- Задача 3 (ML-модель): NEW -> IN_PROGRESS (version = 1)
INSERT INTO task_status_events
(task_id, previous_status, new_status, assignee_id, assignee_email, task_version, comment, occurred_at)
SELECT
    t.id, 'NEW', 'IN_PROGRESS', d.id, d.email, 1, 'Взята в работу', clock_timestamp() - interval '1 day'
FROM tasks t
    JOIN developers d ON d.id = t.assignee_id
WHERE t.title = 'Обучить модель классификации';

-- Задача 4 (Kubernetes): NEW -> IN_PROGRESS (version = 1)
INSERT INTO task_status_events
(task_id, previous_status, new_status, assignee_id, assignee_email, task_version, comment, occurred_at)
SELECT
    t.id, 'NEW', 'IN_PROGRESS', d.id, d.email, 1, 'Взята в работу', clock_timestamp() - interval '4 days'
FROM tasks t
    JOIN developers d ON d.id = t.assignee_id
WHERE t.title = 'Настроить Kubernetes кластер';

-- -----------------------------------------------------------------------
-- Задача 5 (UI компонент): полный цикл с возвратом на уточнение
--   created_at = -6d
--   v1 NEW -> IN_PROGRESS   -5d
--   v2 IN_PROGRESS -> NEW   -4d  (снятие исполнителя)
--   v3 NEW -> IN_PROGRESS   -2d  (assigned_at задачи = -2d)
--   v4 IN_PROGRESS -> DONE  -1d  (= completed_at)
-- Все occurred_at строго возрастают и позже created_at.
-- -----------------------------------------------------------------------
INSERT INTO task_status_events
(task_id, previous_status, new_status, assignee_id, assignee_email, task_version, comment, occurred_at)
SELECT
    t.id, 'NEW', 'IN_PROGRESS', d.id, d.email, 1, 'Взята в работу', clock_timestamp() - interval '5 days'
FROM tasks t
    JOIN developers d ON d.id = t.assignee_id
WHERE t.title = 'Разработать UI компонент таблицы';

INSERT INTO task_status_events
(task_id, previous_status, new_status, assignee_id, assignee_email, task_version, comment, occurred_at)
SELECT
    t.id, 'IN_PROGRESS', 'NEW', d.id, d.email, 2, 'Возвращена на уточнение макета', clock_timestamp() - interval '4 days'
FROM tasks t
    JOIN developers d ON d.id = t.assignee_id
WHERE t.title = 'Разработать UI компонент таблицы';

INSERT INTO task_status_events
(task_id, previous_status, new_status, assignee_id, assignee_email, task_version, comment, occurred_at)
SELECT
    t.id, 'NEW', 'IN_PROGRESS', d.id, d.email, 3, 'Снова в работе после уточнений', clock_timestamp() - interval '2 days'
FROM tasks t
    JOIN developers d ON d.id = t.assignee_id
WHERE t.title = 'Разработать UI компонент таблицы';

INSERT INTO task_status_events
(task_id, previous_status, new_status, assignee_id, assignee_email, task_version, comment, occurred_at)
SELECT
    t.id, 'IN_PROGRESS', 'DONE', d.id, d.email, 4, 'Задача выполнена', t.completed_at
FROM tasks t
         JOIN developers d ON d.id = t.assignee_id
WHERE t.title = 'Разработать UI компонент таблицы';

-- Задача 7 (Документация API): NEW -> CANCELLED (version = 1)
INSERT INTO task_status_events
(task_id, previous_status, new_status, assignee_id, assignee_email, task_version, comment, occurred_at)
SELECT
    t.id, 'NEW', 'CANCELLED', NULL, NULL, 1, 'Нецелесообразно: есть Confluence', clock_timestamp() - interval '3 days'
FROM tasks t
WHERE t.title = 'Написать документацию API';

-- -----------------------------------------------------------------------
-- Задача 11 (Мониторинг и алерты): простой цикл
--   created_at = -10d, v1 -> IN_PROGRESS -9d, v2 -> DONE -7d (= completed_at)
-- -----------------------------------------------------------------------
INSERT INTO task_status_events
(task_id, previous_status, new_status, assignee_id, assignee_email, task_version, comment, occurred_at)
SELECT
    t.id, 'NEW', 'IN_PROGRESS', d.id, d.email, 1, 'Взята в работу', clock_timestamp() - interval '9 days'
FROM tasks t
    JOIN developers d ON d.id = t.assignee_id
WHERE t.title = 'Настроить мониторинг и алерты';

INSERT INTO task_status_events
(task_id, previous_status, new_status, assignee_id, assignee_email, task_version, comment, occurred_at)
SELECT
    t.id, 'IN_PROGRESS', 'DONE', d.id, d.email, 2, 'Дашборды и алерты настроены', t.completed_at
FROM tasks t
         JOIN developers d ON d.id = t.assignee_id
WHERE t.title = 'Настроить мониторинг и алерты';

-- -----------------------------------------------------------------------
-- Задача 12 (Пагинация API каталога): простой цикл
--   created_at = -3d, v1 -> IN_PROGRESS -2d, v2 -> DONE -1d (= completed_at)
-- -----------------------------------------------------------------------
INSERT INTO task_status_events
(task_id, previous_status, new_status, assignee_id, assignee_email, task_version, comment, occurred_at)
SELECT
    t.id, 'NEW', 'IN_PROGRESS', d.id, d.email, 1, 'Взята в работу', clock_timestamp() - interval '2 days'
FROM tasks t
    JOIN developers d ON d.id = t.assignee_id
WHERE t.title = 'Добавить пагинацию в API каталога';

INSERT INTO task_status_events
(task_id, previous_status, new_status, assignee_id, assignee_email, task_version, comment, occurred_at)
SELECT
    t.id, 'IN_PROGRESS', 'DONE', d.id, d.email, 2, 'Пагинация реализована и протестирована', t.completed_at
FROM tasks t
         JOIN developers d ON d.id = t.assignee_id
WHERE t.title = 'Добавить пагинацию в API каталога';

-- Задачи 6, 8, 9, 10 остаются в статусе NEW без исполнителя и без дополнительных событий.