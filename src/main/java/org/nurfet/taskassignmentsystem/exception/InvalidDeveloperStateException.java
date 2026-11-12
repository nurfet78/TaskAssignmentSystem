package org.nurfet.taskassignmentsystem.exception;

import lombok.Getter;

@Getter
public class InvalidDeveloperStateException extends DomainException {

    private final Long developerId;

    private InvalidDeveloperStateException(String message, Long developerId) {
        super(message);
        this.developerId = developerId;
    }

    public static InvalidDeveloperStateException cannotDeactivate(Long developerId, long activeTasks) {
        return new InvalidDeveloperStateException(
                "Разработчика с id=%d нельзя деактивировать: у него %d активных задач"
                        .formatted(developerId, activeTasks),
                developerId
        );
    }
}
