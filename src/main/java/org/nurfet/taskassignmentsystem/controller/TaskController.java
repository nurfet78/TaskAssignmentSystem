package org.nurfet.taskassignmentsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.nurfet.taskassignmentsystem.domain.enums.TaskStatus;
import org.nurfet.taskassignmentsystem.dto.request.AssignTaskRequest;
import org.nurfet.taskassignmentsystem.dto.request.ChangeStatusRequest;
import org.nurfet.taskassignmentsystem.dto.request.CreateTaskRequest;
import org.nurfet.taskassignmentsystem.dto.request.UpdateTaskRequest;
import org.nurfet.taskassignmentsystem.dto.response.*;
import org.nurfet.taskassignmentsystem.service.TaskService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@Validated
@Tag(name = "Tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Создание задачи")
    @ApiResponse(responseCode = "201", description = "Задача создана")
    @ApiResponse(responseCode = "404", description = "Навык не найден")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request));
    }

    @Operation(summary = "Частичное обновление задачи",
            description = "Поля со значением null не изменяются.")
    @ApiResponse(responseCode = "409", description = "Конфликт версий")
    @ApiResponse(responseCode = "404", description = "Задача или навык не найден")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    @PatchMapping("/{id}")
    public ResponseEntity<TaskResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.update(id, request));
    }

    @Operation(summary = "Смена статуса задачи",
            description = "Перевод в IN_PROGRESS возможен только через назначение исполнителя.")
    @ApiResponse(responseCode = "409", description = "Недопустимый переход статуса или конфликт версий")
    @ApiResponse(responseCode = "404", description = "Задача не найдена")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request) {
        return ResponseEntity.ok(taskService.changeStatus(id, request));
    }

    @Operation(summary = "Получить задачу по ID")
    @ApiResponse(responseCode = "404", description = "Задача не найдена")
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getById(id));
    }

    @Operation(summary = "Получить задачи (постранично)",
            description = "Параметры: page (с 0), size (до 100), sort (например, createdAt,desc). "
                    + "С параметром status возвращаются только задачи в этом статусе.")
    @ApiResponse(responseCode = "400", description = "Недопустимый статус или поле сортировки")
    @GetMapping
    public ResponseEntity<PageResponse<TaskResponse>> getAll(
            @RequestParam(required = false) TaskStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(taskService.getAll(status, pageable));
    }

    @Operation(summary = "Получить неназначенные задачи (по приоритету)")
    @GetMapping("/unassigned")
    public ResponseEntity<List<TaskResponse>> getUnassigned() {
        return ResponseEntity.ok(taskService.getUnassigned());
    }

    @Operation(summary = "Автоматическое назначение задачи",
            description = "Выбирается активный разработчик с наибольшим совпадением навыков "
                    + "и наименьшей загрузкой. Только для задач в статусе NEW с указанными навыками.")
    @ApiResponse(responseCode = "409", description = "Назначение невозможно или нет подходящего разработчика")
    @ApiResponse(responseCode = "404", description = "Задача не найдена")
    @PostMapping("/{id}/auto-assign")
    public ResponseEntity<AssignmentResult> autoAssign(@PathVariable Long id) {
        AssignmentResult result = taskService.autoAssign(id);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Ручное назначение задачи разработчику",
            description = "Разработчик должен быть активным и иметь хотя бы один требуемый навык.")
    @ApiResponse(responseCode = "409", description = "Назначение невозможно или конфликт версий")
    @ApiResponse(responseCode = "404", description = "Задача или разработчик не найден")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    @PostMapping("/{id}/assign")
    public ResponseEntity<TaskResponse> assign(
            @PathVariable Long id,
            @Valid @RequestBody AssignTaskRequest request) {

        return ResponseEntity.ok(taskService.assignToDeveloper(id, request));
    }

    @Operation(summary = "Снять исполнителя с задачи",
            description = "Задача в статусе IN_PROGRESS возвращается в NEW.")
    @ApiResponse(responseCode = "409", description = "Задача завершена или конфликт версий")
    @ApiResponse(responseCode = "404", description = "Задача не найдена")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    @DeleteMapping("/{id}/assignee")
    public ResponseEntity<TaskResponse> unassign(
            @PathVariable Long id,
            @RequestParam Long version,
            @RequestParam(required = false) @Size(max = 500) String comment) {

        return ResponseEntity.ok(taskService.unassign(id, version, comment));
    }

    @Operation(summary = "История изменений задачи")
    @ApiResponse(responseCode = "404", description = "Задача не найдена")
    @GetMapping("/{id}/history")
    public ResponseEntity<List<TaskEventResponse>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getHistory(id));
    }

    @Operation(summary = "Удаление задачи",
            description = "Удалять можно только задачи в статусах NEW и CANCELLED.")
    @ApiResponse(responseCode = "204", description = "Задача удалена")
    @ApiResponse(responseCode = "409", description = "Задачу нельзя удалить в текущем статусе")
    @ApiResponse(responseCode = "404", description = "Задача не найдена")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Кандидаты на задачу (пробное автоназначение)",
            description = "Показывает, кого выбрало бы автоназначение, ничего не меняя: сначала полное "
                    + "совпадение навыков, затем больше совпадений, затем меньше активных задач. "
                    + "Пустой список, если у задачи нет навыков или нет подходящих активных разработчиков. "
                    + "К моменту реального назначения результат может измениться.")
    @ApiResponse(responseCode = "404", description = "Задача не найдена")
    @ApiResponse(responseCode = "400", description = "Недопустимый limit")
    @GetMapping("/{id}/candidates")
    public ResponseEntity<List<CandidateResponse>> getCandidates(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {

        return ResponseEntity.ok(taskService.getCandidates(id, limit));
    }

}
