package org.nurfet.taskassignmentsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.nurfet.taskassignmentsystem.domain.entity.TaskStatusEvent;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskStatusEventRepository extends JpaRepository<TaskStatusEvent, Long> {

    List<TaskStatusEvent> findByTaskIdOrderByOccurredAtDesc(Long taskId);
}