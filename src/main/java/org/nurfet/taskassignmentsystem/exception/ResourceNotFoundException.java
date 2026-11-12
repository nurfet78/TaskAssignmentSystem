package org.nurfet.taskassignmentsystem.exception;

import lombok.Getter;

import javax.swing.*;

@Getter
public class ResourceNotFoundException extends DomainException {

    private final String resourceType;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceType, Object resourceId, String message) {
        super(message);

        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public static ResourceNotFoundException task(Long id) {
        return new ResourceNotFoundException("Task", id,
                "Задача с идентификатором %d не найдена".formatted(id));
    }

    public static ResourceNotFoundException developer(Long id) {
        return new ResourceNotFoundException("Developer", id,
                "Разработчик с идентификатором %d не найден".formatted(id));
    }

    public static ResourceNotFoundException developerEmail(String email) {
        return new ResourceNotFoundException("Developer", "email",
                "Разработчик с email %s не найден".formatted(email));
    }

    public static ResourceNotFoundException skill(Long id) {
        return new ResourceNotFoundException("Skill", id,
                "Навык с идентификатором %d не найден".formatted(id));
    }

    public static ResourceNotFoundException skillByName(String name) {
        return new ResourceNotFoundException("Skill", name,
                "Навык с названием %s не найден".formatted(name));
    }
}