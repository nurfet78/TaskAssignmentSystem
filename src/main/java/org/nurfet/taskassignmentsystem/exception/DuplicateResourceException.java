package org.nurfet.taskassignmentsystem.exception;

import lombok.Getter;

@Getter
public class DuplicateResourceException extends DomainException {

    private final String resourceType;
    private final String field;
    private final Object value;

    public DuplicateResourceException(String resourceType, String field, Object value, String message) {
        super(message);

        this.resourceType = resourceType;
        this.field = field;
        this.value = value;
    }

    public static DuplicateResourceException developerEmail(String email) {
        return new DuplicateResourceException("Developer", "email", email,
                "Email %s уже используется другим пользователем".formatted(email));
    }

    public static DuplicateResourceException skillName(String name) {
        return new DuplicateResourceException("Skill", "name", name,
                "Название навыка %s уже существует".formatted(name));
    }
}