package org.nurfet.taskassignmentsystem.mapper;

import org.mapstruct.Named;
import org.nurfet.taskassignmentsystem.domain.entity.Developer;
import org.nurfet.taskassignmentsystem.domain.entity.Skill;
import org.nurfet.taskassignmentsystem.domain.entity.Task;
import org.nurfet.taskassignmentsystem.dto.response.DeveloperSummary;
import org.nurfet.taskassignmentsystem.dto.response.SkillResponse;
import org.nurfet.taskassignmentsystem.dto.response.TaskResponse;
import org.nurfet.taskassignmentsystem.dto.request.CreateTaskRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TaskMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "requiredSkills", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "assignedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    Task toEntity(CreateTaskRequest request);

    @Mapping(target = "assignee", source = "assignee", qualifiedByName = "developerToSummary")
    @Mapping(target = "requiredSkills", source = "requiredSkills", qualifiedByName = "skillsToResponse")
    TaskResponse toResponse(Task task);

    List<TaskResponse> toResponseList(List<Task> tasks);

    @Named("developerToSummary")
    default DeveloperSummary developerToSummary(Developer developer) {
        if (developer == null) {
            return null;
        }

        return new DeveloperSummary(
                developer.getId(),
                developer.getFullName(),
                developer.getEmail()
        );
    }

    @Named("skillsToResponse")
    default Set<SkillResponse> skillsToResponse(Set<Skill> skills) {
        if (skills == null) {
            return Set.of();
        }

        return skills.stream()
                .map(s -> new SkillResponse(s.getId(), s.getName(), s.getDescription()))
                .collect(Collectors.toSet());
    }
}