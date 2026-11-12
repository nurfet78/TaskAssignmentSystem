package org.nurfet.taskassignmentsystem.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import org.nurfet.taskassignmentsystem.domain.entity.TaskStatusEvent;
import org.nurfet.taskassignmentsystem.dto.response.TaskEventResponse;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TaskEventMapper {

    TaskEventResponse toResponse(TaskStatusEvent event);

    List<TaskEventResponse> toResponseList(List<TaskStatusEvent> events);
}