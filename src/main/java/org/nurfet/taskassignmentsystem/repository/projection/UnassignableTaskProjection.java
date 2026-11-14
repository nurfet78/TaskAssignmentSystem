package org.nurfet.taskassignmentsystem.repository.projection;

public interface UnassignableTaskProjection {
    Long getTaskId();
    String getTitle();
    String getPriority();
    long getRequiredSkillsCount();
}
