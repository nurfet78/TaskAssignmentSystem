package org.nurfet.taskassignmentsystem.repository;

import org.nurfet.taskassignmentsystem.domain.entity.Task;
import org.nurfet.taskassignmentsystem.repository.projection.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

@org.springframework.stereotype.Repository
public interface ReportRepository extends Repository<Task, Long> {

    @Query(value = """
    select status as status,
    count(*) as taskCount
    from tasks
    group by status
""", nativeQuery = true)
    List<StatusCountProjection> countTasksByStatus();

    @Query(value = """
    select
        t.priority as priority,
        count(*) as completedTasks,
        round(cast(avg(extract(epoch from (t.completed_at - t.created_at))) / 3600.0 as numeric), 1) as avgHours,
        round(cast(min(extract(epoch from (t.completed_at - t.created_at))) / 3600.0 as numeric), 1) as minHours,
        round(cast(max(extract(epoch from (t.completed_at - t.created_at))) / 3600.0 as numeric), 1) as maxHours
    from tasks t
    where t.status = 'DONE'
      and t.completed_at is not null
    group by t.priority
""", nativeQuery = true)
    List<PriorityCompletionProjection> findCompletionStatsByPriority();

    @Query(value = """
    select
        t.id as taskId,
        t.title as title,
        t.priority as priority,
        (select count(*) from task_skills ts where ts.task_id = t.id) as requiredSkillsCount
    from tasks t
    where t.status = 'NEW'
      and t.assignee_id is null
      and exists (
          select 1 from task_skills ts where ts.task_id = t.id
      )
      and not exists (
          select 1
          from task_skills ts
          join developer_skills ds on ds.skill_id = ts.skill_id
          join developers d on d.id = ds.developer_id
          where ts.task_id = t.id
            and d.active
      )
    order by
        case t.priority
            when 'CRITICAL' then 1
            when 'HIGH' then 2
            when 'MEDIUM' then 3
            else 4
        end,
        t.created_at, t.id
""", nativeQuery = true)
    List<UnassignableTaskProjection> findUnassignableTasks();

    /*
     * События смены исполнителя (previous_status = new_status) не меняют статус,
     * их убираем до lead(): иначе они рвали бы интервал и считались новым входом в статус.
     * Событие создания (previous_status is null) остаётся.
     * Конечный статус (DONE, CANCELLED) не имеет конца, поэтому время в нём не считаем (null).
     * Текущий момент передаётся параметром :now.
     */
    @Query(value = """
    with status_changes as (
        select id, new_status, occurred_at
        from task_status_events
        where task_id = :taskId
          and (previous_status is null or previous_status <> new_status)
    ),
    intervals as (
        select
            new_status,
            occurred_at,
            lead(occurred_at) over (order by occurred_at, id) as left_at
        from status_changes
    )
    select
        new_status as status,
        count(*) as timesEntered,
        cast(sum(
            case
                when left_at is null and new_status in ('DONE', 'CANCELLED') then null
                else extract(epoch from (coalesce(left_at, cast(:now as timestamptz)) - occurred_at))
            end
        ) as bigint) as totalSeconds,
        bool_or(left_at is null) as isCurrent
    from intervals
    group by new_status
    order by min(occurred_at)
""", nativeQuery = true)
    List<StatusDurationProjection> findStatusDurations(@Param("taskId") Long taskId,
                                                       @Param("now") Instant now);

    @Query(value = """
    select priority as priority,
           count(*) as taskCount
    from tasks
    group by priority
""", nativeQuery = true)
    List<PriorityCountProjection> countTasksByPriority();
}
