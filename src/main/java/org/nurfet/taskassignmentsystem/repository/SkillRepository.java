package org.nurfet.taskassignmentsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.nurfet.taskassignmentsystem.domain.entity.Skill;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    @Query("select s from Skill s where lower(s.name) = lower(:name)")
    Optional<Skill> findByNameIgnoreCase(@Param("name") String name);

    @Query("""
        select exists (
            select 1 from Skill s where lower(s.name) = lower(:name)
        )
    """)
    boolean existsByNameIgnoreCase(@Param("name") String name);

    Set<Skill> findByIdIn(Collection<Long> ids);

    @Query(value = """
        select exists(select 1 from developer_skills where skill_id = :skillId)
            or exists(select 1 from task_skills where skill_id = :skillId)
    """, nativeQuery = true)
    boolean isInUse(@Param("skillId") Long skillId);
}