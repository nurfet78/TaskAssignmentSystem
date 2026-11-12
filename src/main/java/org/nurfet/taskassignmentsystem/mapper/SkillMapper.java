package org.nurfet.taskassignmentsystem.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.nurfet.taskassignmentsystem.domain.entity.Skill;
import org.nurfet.taskassignmentsystem.dto.request.CreateSkillRequest;
import org.nurfet.taskassignmentsystem.dto.response.SkillResponse;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SkillMapper {

    Skill toEntity(CreateSkillRequest request);

    SkillResponse toResponse(Skill skill);

    List<SkillResponse> toResponseList(List<Skill> skills);
}