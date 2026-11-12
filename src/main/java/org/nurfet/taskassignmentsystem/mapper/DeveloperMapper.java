package org.nurfet.taskassignmentsystem.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.nurfet.taskassignmentsystem.domain.entity.Developer;
import org.nurfet.taskassignmentsystem.domain.entity.Skill;
import org.nurfet.taskassignmentsystem.dto.request.CreateDeveloperRequest;
import org.nurfet.taskassignmentsystem.dto.response.DeveloperResponse;
import org.nurfet.taskassignmentsystem.dto.response.SkillResponse;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DeveloperMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "skills", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Developer toEntity(CreateDeveloperRequest request);

    @Mapping(target = "skills", source = "skills", qualifiedByName = "skillsToResponse")
    @Mapping(target = "activeTaskCount", constant = "0L")
    DeveloperResponse toResponse(Developer developer);

    @Named("skillsToResponse")
    default Set<SkillResponse> skillsToResponse(Set<Skill> skills) {
        if (skills == null) {
            return Set.of();
        }

        return skills.stream()
                .map(s -> new SkillResponse(s.getId(), s.getName(), s.getDescription()))
                .collect(Collectors.toSet());
    }

    default DeveloperResponse toResponseWithTaskCount(Developer developer, long activeTaskCount) {
        Set<SkillResponse> skillResponses = skillsToResponse(developer.getSkills());

        return new DeveloperResponse(
                developer.getId(),
                developer.getVersion(),
                developer.getFirstName(),
                developer.getLastName(),
                developer.getFullName(),
                developer.getEmail(),
                developer.isActive(),
                skillResponses,
                activeTaskCount,
                developer.getCreatedAt(),
                developer.getUpdatedAt()
        );
    }
}