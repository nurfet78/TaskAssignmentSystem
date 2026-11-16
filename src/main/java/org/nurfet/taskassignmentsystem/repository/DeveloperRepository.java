package org.nurfet.taskassignmentsystem.repository;

import jakarta.persistence.LockModeType;
import org.nurfet.taskassignmentsystem.repository.projection.DeveloperMatchProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.nurfet.taskassignmentsystem.domain.entity.Developer;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeveloperRepository extends JpaRepository<Developer, Long>, JpaSpecificationExecutor<Developer> {

    Optional<Developer> findByEmail(String email);

    boolean existsByEmail(String email);



    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Developer d where d.id = :id")
    Optional<Developer> findByIdWithLock(@Param("id")Long id);

    @Query(value = """
    with required_skills as (
        select skill_id
        from task_skills
        where task_id = :taskId
    ),
    skills_count as (
        select count(*) as total
        from required_skills
    ),
    developer_matched_skills as (
        select
            developer_id,
            count(*) as matched_skills
        from developer_skills ds
        join required_skills rs on ds.skill_id = rs.skill_id
        join developers d on ds.developer_id = d.id
        where d.active = true
        group by developer_id
    ),
    developer_active_tasks as (
        select
            assignee_id,
            count(*) as active_tasks
        from tasks t
        where t.status in('NEW', 'IN_PROGRESS')
        and t.assignee_id is not null
        group by assignee_id
    )
    select
        d.id as developerId,
        d.first_name as firstName,
        d.last_name as lastName,
        d.email as email,
        dms.matched_skills as matchedSkills,
        (dms.matched_skills = sc.total) as isExactMatch,
        sc.total as requiredSkills,
        coalesce(dat.active_tasks, 0) as activeTasks
    from developers d
    join developer_matched_skills dms on d.id = dms.developer_id
    left join developer_active_tasks dat on d.id = dat.assignee_id
    cross join skills_count sc
    order by
        isExactMatch desc,
        matchedSkills desc,
        activeTasks,
        developerId
    limit :limit
""", nativeQuery = true)
    List<DeveloperMatchProjection> findBestCandidatesForTask(@Param("taskId")Long taskId,
                                                             @Param("limit")int limit);
}