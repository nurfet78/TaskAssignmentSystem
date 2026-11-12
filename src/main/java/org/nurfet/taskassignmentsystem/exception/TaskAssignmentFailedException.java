package org.nurfet.taskassignmentsystem.exception;

import lombok.Getter;

@Getter
public class TaskAssignmentFailedException extends DomainException {

    private final Long taskId;
    private final String reason;

    private TaskAssignmentFailedException(Long taskId, String reason) {
        super("Не удалось назначить задачу %d: %s".formatted(taskId, reason));

        this.taskId = taskId;
        this.reason = reason;
    }

    public static TaskAssignmentFailedException noMatchingDeveloper(Long taskId) {
        return new TaskAssignmentFailedException(taskId,
                "Не найден разработчик с необходимыми навыками");
    }

    public static TaskAssignmentFailedException taskNotNew(Long taskId) {
        return new TaskAssignmentFailedException(taskId,
                "Задача должна быть в статусе 'NEW' для автоматического назначения");
    }

    public static TaskAssignmentFailedException alreadyAssigned(Long taskId) {
        return new TaskAssignmentFailedException(taskId,
                "Задача уже назначена исполнителю");
    }

    public static TaskAssignmentFailedException noRequiredSkills(Long taskId) {
        return new TaskAssignmentFailedException(taskId,
                "Для задачи не указаны необходимые навыки");
    }

    public static TaskAssignmentFailedException developerInactive(Long taskId, String developerEmail) {
        return new TaskAssignmentFailedException(taskId,
                "Разработчик %s неактивен".formatted(developerEmail));
    }
}