package org.nurfet.taskassignmentsystem.repository.projection;

public interface StatusDurationProjection {
    String getStatus();
    long getTimesEntered();
    Long getTotalSeconds();
    Boolean getIsCurrent();
}
