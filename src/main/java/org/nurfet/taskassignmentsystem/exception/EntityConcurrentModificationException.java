package org.nurfet.taskassignmentsystem.exception;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

import lombok.Getter;

@Getter
public class EntityConcurrentModificationException extends DomainException {

    private final String entityType;
    private final Long entityId;

    public EntityConcurrentModificationException(String entityType, Long entityId) {
        super("""
                Ресурс %s с идентификатором %d был изменен другим процессом.
                Повторите попытку.
                """.formatted(entityType, entityId));

        this.entityType = entityType;
        this.entityId = entityId;
    }

    public static EntityConcurrentModificationException developer(Long id) {
        return new EntityConcurrentModificationException("Developer", id);
    }

    public static EntityConcurrentModificationException task(Long id) {
        return new EntityConcurrentModificationException("Task", id);
    }
}