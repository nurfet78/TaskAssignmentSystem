package org.nurfet.taskassignmentsystem.repository;

import org.nurfet.taskassignmentsystem.domain.entity.Task;
import org.nurfet.taskassignmentsystem.repository.projection.PriorityCompletionProjection;
import org.nurfet.taskassignmentsystem.repository.projection.StatusCountProjection;
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
}
