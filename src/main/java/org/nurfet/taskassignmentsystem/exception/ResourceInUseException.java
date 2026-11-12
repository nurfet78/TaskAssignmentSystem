package org.nurfet.taskassignmentsystem.exception;

public class ResourceInUseException extends DomainException {

    private ResourceInUseException(String message) {
        super(message);
    }

    public static ResourceInUseException skill(Long id) {
        return new ResourceInUseException(
                "Навык с id=%d используется разработчиками или задачами и не может быть удалён"
                        .formatted(id));
    }
}
