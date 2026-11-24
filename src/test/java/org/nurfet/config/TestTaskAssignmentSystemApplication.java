package org.nurfet.config;

import org.nurfet.taskassignmentsystem.TaskAssignmentSystemApplication;
import org.springframework.boot.SpringApplication;

public class TestTaskAssignmentSystemApplication {

    public static void main(String[] args) {
        SpringApplication
                .from(TaskAssignmentSystemApplication::main)
                .with(ContainersConfig.class, DevModeBannerConfig.class)
                .withAdditionalProfiles("dev")
                .run(args);
    }
}
