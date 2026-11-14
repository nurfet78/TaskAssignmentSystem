package org.nurfet.taskassignmentsystem.repository.projection;

import java.math.BigDecimal;

public interface PriorityCompletionProjection {
    String getPriority();
    long getCompletedTasks();
    BigDecimal getAvgHours();
    BigDecimal getMinHours();
    BigDecimal getMaxHours();
}
