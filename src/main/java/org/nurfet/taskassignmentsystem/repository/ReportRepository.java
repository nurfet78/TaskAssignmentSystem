package org.nurfet.taskassignmentsystem.repository;

import org.nurfet.taskassignmentsystem.domain.entity.Task;
import org.nurfet.taskassignmentsystem.repository.projection.PriorityCompletionProjection;
import org.nurfet.taskassignmentsystem.repository.projection.StatusCountProjection;
import org.nurfet.taskassignmentsystem.repository.projection.UnassignableTaskProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

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
}
