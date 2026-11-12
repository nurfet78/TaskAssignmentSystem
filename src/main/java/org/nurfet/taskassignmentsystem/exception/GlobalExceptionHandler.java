package org.nurfet.taskassignmentsystem.exception;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String ERROR_BASE_URI = "/error/";

    private record FieldValidationError(String field, String message, Object rejectedValue) {}

    private static String extractFieldName(Path propertyPath) {
        String fullPath = propertyPath.toString();
        int dot = fullPath.lastIndexOf('.');
        return dot >= 0 ? fullPath.substring(dot + 1) : fullPath;
    }

    private static String simpleClassName(String fullName) {
        if (fullName == null) {
            return null;
        }
        int dot = fullName.lastIndexOf('.');
        return dot >= 0 ? fullName.substring(dot + 1) : fullName;
    }

    private static Object safeRejectedValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String
        || value instanceof Number
        || value instanceof Boolean) {

            return value;
        }

        return value.toString();
    }

    private static ProblemDetail buildProblem(HttpStatus status, String detail,
                                              String title, String errorType) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(ERROR_BASE_URI + errorType));
        problem.setProperty("errorCode", errorType.toUpperCase(Locale.ROOT));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        var fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(s -> new FieldValidationError(
                        s.getField(),
                        Objects.requireNonNullElse(s.getDefaultMessage(), "Invalid value"),
                        safeRejectedValue(s.getRejectedValue())
                )).toList();

        log.info("Validation failed. Errors: {}", fieldErrors.size());

        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "Одно или несколько правил валидации были нарушены",
                "Ошибка валидации",
                "validation_error");

        problem.setProperty("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().headers(headers).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {

        var fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(s -> new FieldValidationError(
                        extractFieldName(s.getPropertyPath()),
                        Objects.requireNonNullElse(s.getMessage(), "Invalid value"),
                        safeRejectedValue(s.getInvalidValue())
                )).toList();

        log.info("Violation failed. Errors: {}", fieldErrors.size());

        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "Одно или несколько правил валидации были нарушены",
                "Ошибка валидации",
                "validation_error"
        );

        problem.setProperty("fieldErrors", fieldErrors);

        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.info("Ошибка оптимистической блокировки {}", ex.getMessage());

        ProblemDetail problem = buildProblem(
                HttpStatus.CONFLICT,
                "Ресурс был изменен другим процессом. Повторите попытку",
                "Ошибка оптимистической блокировки",
                "optimistic_lock"
        );

        problem.setProperty("entityType", simpleClassName(ex.getPersistentClassName()));
        problem.setProperty("entityId", ex.getIdentifier());

        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        String constraint = (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException cve)
                ? cve.getConstraintName()
                : null;

        log.warn("Нарушение целостности данных: constraint={}, cause={}",
                constraint, ex.getMostSpecificCause().getClass().getSimpleName());

        return buildProblem(
                HttpStatus.CONFLICT,
                "Операция нарушает ограничения целостности данных",
                "Нарушение целостности данных",
                "data_integrity"
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.info("Недопустимые параметры запроса {}", ex.getMessage());

        return buildProblem(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                "Недопустимые параметры запроса",
                "invalid_request"
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Непредвиденная ошибка", ex);

        return buildProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Произошла непредвиденная ошибка. Пожалуйста повторите попытку позже",
                "Непредвиденная ошибка",
                "internal_error"
        );
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex) {
        log.warn("Нарушено бизнес-правило: {}", ex.getMessage());

        return buildProblem(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                "Нарушено бизнес-правило",
                "business_rule"
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicateResource(DuplicateResourceException ex) {
        log.info("Ресурс уже существует {}", ex.getMessage());

        ProblemDetail problem = buildProblem(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                "Ресурс уже существует",
                "duplicate"
        );

        problem.setProperty("resourceType", ex.getResourceType());
        problem.setProperty("field", ex.getField());
        problem.setProperty("value", ex.getValue());

        return problem;
    }

    @ExceptionHandler(EntityConcurrentModificationException.class)
    public ProblemDetail handleEntityConcurrentModification(EntityConcurrentModificationException ex) {
        log.info("Ошибка оптимистической блокировки {}", ex.getMessage());

        ProblemDetail problem = buildProblem(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                "Ресурс был изменен другим процессом",
                "optimistic_lock"
        );

        problem.setProperty("entityId", ex.getEntityId());
        problem.setProperty("entityType", ex.getEntityType());

        return problem;
    }

    @ExceptionHandler(InvalidDeveloperStateException.class)
    public ProblemDetail handleInvalidDeveloperState(InvalidDeveloperStateException ex) {
        log.info("Не удалось деактивировать разработчика {}", ex.getMessage());

        ProblemDetail problem = buildProblem(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                "Не удалось деактивировать разработчика",
                "failed_deactivate"
        );

        problem.setProperty("developerId", ex.getDeveloperId());

        return problem;
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ProblemDetail handleInvalidStatus(InvalidStatusTransitionException ex) {
        log.info("Недопустимый переход статусов {}", ex.getMessage());

        ProblemDetail problem = buildProblem(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                "Недопустимый переход статусов",
                "invalid_transition"
        );

        problem.setProperty("currentStatus", ex.getCurrentStatus());
        problem.setProperty("targetStatus", ex.getTargetStatus());

        return problem;
    }

    @ExceptionHandler(InvalidTaskStateException.class)
    public ProblemDetail handleInvalidTaskState(InvalidTaskStateException ex) {
        log.info("Ошибка удаления статуса {}", ex.getMessage());

        ProblemDetail problem = buildProblem(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                "Ошибка удаления статуса",
                "error_deleting_status"
        );

        problem.setProperty("taskId", ex.getTaskId());
        problem.setProperty("currentStatus", ex.getCurrentStatus());

        return problem;
    }

    @ExceptionHandler(MissingRequiredSkillsException.class)
    public ProblemDetail handleMissingRequiredSkills(MissingRequiredSkillsException ex) {
        log.info("Отсутствие необходимых навыков {}", ex.getMessage());

        ProblemDetail problem = buildProblem(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                "Отсутствие необходимых навыков",
                "required_skills"
        );

        problem.setProperty("taskId", ex.getTaskId());
        problem.setProperty("developerEmail", ex.getDeveloperEmail());

        return problem;
    }

    @ExceptionHandler(ResourceInUseException.class)
    public ProblemDetail handleResourceInUse(ResourceInUseException ex) {
        log.info("Ресурс используется {}", ex.getMessage());

        return buildProblem(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                "Нельзя удалить навык",
                "resource_in_use"
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        log.info("Ресурс не найден {}", ex.getMessage());

        ProblemDetail problem = buildProblem(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                "Ресурс не найден",
                "not_found"
        );

        problem.setProperty("resourceType", ex.getResourceType());
        problem.setProperty("resourceId", ex.getResourceId());

        return problem;
    }

    @ExceptionHandler(TaskAssignmentFailedException.class)
    public ProblemDetail handleTaskAssignmentFailed(TaskAssignmentFailedException ex) {
        log.info("Не удалось назначить задачу {}", ex.getMessage());

        ProblemDetail problem = buildProblem(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                "Не удалось назначить задачу",
                "assignment_failed"
        );

        problem.setProperty("taskId", ex.getTaskId());
        problem.setProperty("reason", ex.getReason());

        return problem;
    }

    @ExceptionHandler(TaskHasNoAssigneeException.class)
    public ProblemDetail handleTaskHasNoAssignee(TaskHasNoAssigneeException ex) {
        log.info("Запрещено менять статус {}", ex.getMessage());

        ProblemDetail problem = buildProblem(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                "Запрещено менять статус",
                "transition_failed"
        );

        problem.setProperty("taskId", ex.getTaskId());

        return problem;
    }

    @ExceptionHandler(TaskInTerminalStatusException.class)
    public ProblemDetail handleTerminalStatus(TaskInTerminalStatusException ex) {
        log.info("Задача находится в терминальном статусе {}", ex.getMessage());

        ProblemDetail problem = buildProblem(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                "Задача находится в терминальном статусе",
                "terminal_status"
        );

        problem.setProperty("taskId", ex.getTaskId());
        problem.setProperty("status", ex.getStatus());

        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Throwable cause = ex.getCause();

        // Недопустимое значение enum, например priority = "URGENT"
        if (cause instanceof InvalidFormatException ife
                && ife.getTargetType() != null
                && ife.getTargetType().isEnum()) {

            String field = fieldPath(ife);
            String allowed = Arrays.stream(ife.getTargetType().getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));

            return badRequest(headers, field,
                    "Допустимые значения: " + allowed,
                    ife.getValue());
        }

        // Значение не того типа, например строка вместо числа в skillIds или version
        if (cause instanceof MismatchedInputException mie) {
            return badRequest(headers, fieldPath(mie), "Значение имеет неверный тип или формат", null);
        }

        // Пустое тело, синтаксическая ошибка JSON и прочее
        return badRequest(headers, null,
                "Тело запроса отсутствует или содержит некорректный JSON", null);
    }

    /**
     * Строит ответ в формате остальных ошибок валидации.
     * Текст исключения Jackson клиенту не отдаём: в нём есть имена внутренних классов.
     */
    private ResponseEntity<Object> badRequest(HttpHeaders headers, String field,
                                              String message, Object rejectedValue) {

        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "Одно или несколько правил валидации были нарушены",
                "Ошибка валидации",
                "validation_error");

        if (field != null && !field.isBlank()) {
            problem.setProperty("fieldErrors",
                    List.of(new FieldValidationError(field, message, safeRejectedValue(rejectedValue))));
        } else {
            problem.setDetail(message);
        }

        return ResponseEntity.badRequest().headers(headers).body(problem);
    }

    private static String fieldPath(MismatchedInputException e) {
        return e.getPath().stream()
                .map(JacksonException.Reference::getPropertyName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));
    }

    // =========================================================================
    // Неверный тип параметра запроса или переменной пути:
    //   GET /tasks?status=UNKNOWN   (enum)
    //   GET /tasks/abc              (Long)
    // =========================================================================

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String field = ex instanceof MethodArgumentTypeMismatchException m
                ? m.getName()
                : ex.getPropertyName();

        Class<?> type = ex.getRequiredType();

        String message = (type != null && type.isEnum())
                ? "Допустимые значения: " + Arrays.stream(type.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "))
                : "Значение имеет неверный тип или формат";

        return badRequest(headers, field, message, ex.getValue());
    }
}