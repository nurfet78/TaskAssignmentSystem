package org.nurfet.taskassignmentsystem.exception;

import lombok.Getter;

@Getter
public class MissingRequiredSkillsException extends DomainException {

    private final Long taskId;
    private final String developerEmail;

    public MissingRequiredSkillsException(Long taskId, String developerEmail) {
        super("У разработчика %s нет навыков для решения задачи %d".formatted(developerEmail, taskId));

        this.taskId = taskId;
        this.developerEmail = developerEmail;
    }
}