package org.nurfet.taskassignmentsystem.util;

public final class LikeUtils {

    private LikeUtils() {}

    public static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("%", "\\%").replace("_", "\\_");
    }
}
