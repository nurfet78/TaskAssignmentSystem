package org.nurfet.taskassignmentsystem.repository.projection;

public interface DeveloperMatchProjection {
    Long getDeveloperId();
    String getFirstName();
    String getLastName();
    String getEmail();
    Long getMatchedSkills();
    Boolean getIsExactMatch();
    Long getRequiredSkills();
    Long getActiveTasks();
}