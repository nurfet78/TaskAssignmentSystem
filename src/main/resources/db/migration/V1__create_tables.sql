create table skills (
    id bigserial primary key,
    name varchar(100) not null,
    description varchar(500)
    );

create unique index idx_skill_name_lower on skills (lower(name));

create table developers (
    id bigserial primary key,
    version bigint not null default 0,
    first_name varchar(100) not null,
    last_name varchar(100) not null,
    email varchar(254) not null,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_developers_email_lower check (email = lower(email))
    );

create unique index uk_developers_email on developers (email);

create table developer_skills (
    developer_id bigint not null references developers(id) on delete cascade,
    skill_id bigint not null references skills(id) on delete restrict,
    primary key (developer_id, skill_id)
    );

create index idx_developer_skills_skill on developer_skills(skill_id);

create table tasks (
    id bigserial primary key,
    version bigint not null default 0,
    title varchar(200) not null,
    description varchar(2000),
    status varchar(20) not null default 'NEW',
    priority varchar(20) not null default 'MEDIUM',
    assignee_id bigint references developers(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    assigned_at timestamptz,
    completed_at timestamptz,
    constraint chk_task_status check ( status in('NEW', 'IN_PROGRESS', 'DONE', 'CANCELLED') ),
    constraint chk_task_priority check ( priority in('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') )
    );

create index idx_task_assignee_id on tasks(assignee_id);
create index idx_task_created_at on tasks(created_at);
create index idx_task_assignee_status on tasks(assignee_id, status);

create table task_skills (
    task_id bigint not null references tasks(id) on delete cascade,
    skill_id bigint not null references skills(id) on delete restrict,
    primary key (task_id, skill_id)
    );

create index idx_task_skills_skill on task_skills(skill_id);

create table task_status_events (
    id bigserial primary key,
    task_id bigint not null,
    previous_status varchar(20),
    new_status varchar(20) not null,
    assignee_id bigint,
    assignee_email varchar(254),
    occurred_at timestamptz not null default now(),
    comment varchar(500),
    task_version bigint,
    constraint chk_event_prev_status check ( previous_status in('NEW', 'IN_PROGRESS', 'DONE', 'CANCELLED') ),
    constraint chk_event_new_status check ( new_status in('NEW', 'IN_PROGRESS', 'DONE', 'CANCELLED') )
    );

create index idx_occurred_at on task_status_events(occurred_at);
create index idx_task_id_occurred_at on task_status_events(task_id, occurred_at);