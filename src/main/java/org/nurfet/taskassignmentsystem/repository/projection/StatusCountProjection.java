package org.nurfet.taskassignmentsystem.repository.projection;

public interface StatusCountProjection {
    String getStatus();
    long getTaskCount();
}
