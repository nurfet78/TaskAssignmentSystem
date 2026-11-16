package org.nurfet.taskassignmentsystem.repository;

import org.jspecify.annotations.NullMarked;
import org.nurfet.taskassignmentsystem.domain.entity.Task;
import org.nurfet.taskassignmentsystem.repository.projection.AssigneeTaskCount;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @EntityGraph(attributePaths = {"requiredSkills", "assignee"})
    @Query("""
    select t
    from Task t
    where t.id = :taskId
""")
    Optional<Task> findByIdWithDetails(@Param("taskId")Long taskId);

    @Override
    @EntityGraph(attributePaths = "assignee")
    @NullMarked
    Page<Task> findAll(Specification<Task> spec, Pageable pageable);

    @Query("""
    select count(t)
    from Task t
    where t.assignee.id = :assigneeId
    and t.status in(org.nurfet.taskassignmentsystem.domain.enums.TaskStatus.NEW,
                    org.nurfet.taskassignmentsystem.domain.enums.TaskStatus.IN_PROGRESS)
""")
    long countActiveTaskByAssigneeId(@Param("assigneeId")Long assigneeId);

    @Query("""
    select t.assignee.id as assigneeId, count(t) as taskCount
    from Task t
    where t.assignee.id in :assigneeIds
      and t.status in (
          org.nurfet.taskassignmentsystem.domain.enums.TaskStatus.NEW,
          org.nurfet.taskassignmentsystem.domain.enums.TaskStatus.IN_PROGRESS)
    group by t.assignee.id
""")
    List<AssigneeTaskCount> countActiveTasksByAssigneeIds(@Param("assigneeIds") Collection<Long> assigneeIds);


    @EntityGraph(attributePaths = {"requiredSkills", "assignee"})
    @Query("""
    select t
    from Task t
    where t.assignee is null
    and t.status = org.nurfet.taskassignmentsystem.domain.enums.TaskStatus.NEW
    order by
        case t.priority
    when org.nurfet.taskassignmentsystem.domain.enums.TaskPriority.CRITICAL then 1
    when org.nurfet.taskassignmentsystem.domain.enums.TaskPriority.HIGH then 2
    when org.nurfet.taskassignmentsystem.domain.enums.TaskPriority.MEDIUM then 3
    else 4
    end, t.createdAt asc, t.id asc
""")
    List<Task> findUnassignedOrderByPriority();
}