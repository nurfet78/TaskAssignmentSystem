package org.nurfet.taskassignmentsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.nurfet.taskassignmentsystem.dto.request.ChangeEmailRequest;
import org.nurfet.taskassignmentsystem.dto.request.CreateDeveloperRequest;
import org.nurfet.taskassignmentsystem.dto.request.UpdateDeveloperRequest;
import org.nurfet.taskassignmentsystem.dto.response.DeveloperResponse;
import org.nurfet.taskassignmentsystem.dto.response.PageResponse;
import org.nurfet.taskassignmentsystem.service.DeveloperService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/developers")
@RequiredArgsConstructor
@Tag(name = "Developers")
@Validated
public class DeveloperController {

    private final DeveloperService developerService;

    @Operation(summary = "Создание разработчика")
    @ApiResponse(responseCode = "201", description = "Разработчик создан")
    @ApiResponse(responseCode = "409", description = "Email уже используется")
    @ApiResponse(responseCode = "404", description = "Навык не найден")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    @PostMapping
    public ResponseEntity<DeveloperResponse> create(@Valid @RequestBody CreateDeveloperRequest request) {
        DeveloperResponse response = developerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Частичное обновление разработчика",
            description = "Поля со значением null не изменяются. Деактивация (active = false) "
                    + "запрещена, пока у разработчика есть активные задачи.")
    @ApiResponse(responseCode = "409", description = "Конфликт версий или есть активные задачи")
    @ApiResponse(responseCode = "404", description = "Разработчик или навык не найден")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    @PatchMapping("/{id}")
    public ResponseEntity<DeveloperResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeveloperRequest request) {

        return ResponseEntity.ok(developerService.update(id, request));
    }

    @Operation(summary = "Получить всех разработчиков",
            description = "activeOnly=true: только активные. "
                    + "q: подстрока в имени, фамилии, «Имя Фамилия» или email без учёта регистра.")
    @GetMapping
    public ResponseEntity<PageResponse<DeveloperResponse>> getAll(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(required = false) @Size(max = 100) String q,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {

        return ResponseEntity.ok(developerService.getAll(activeOnly, q, pageable));
    }

    @Operation(summary = "Смена email разработчика")
    @ApiResponse(responseCode = "409", description = "Email уже используется или конфликт версий")
    @ApiResponse(responseCode = "404", description = "Разработчик не найден")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    @PatchMapping("/{id}/email")
    public ResponseEntity<DeveloperResponse> changeEmail(
            @PathVariable Long id,
            @Valid @RequestBody ChangeEmailRequest request) {

        return ResponseEntity.ok(developerService.changeEmail(id, request));
    }

    @Operation(summary = "Получить разработчика по ID")
    @ApiResponse(responseCode = "404", description = "Разработчик не найден")
    @GetMapping("/{id}")
    public ResponseEntity<DeveloperResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(developerService.getById(id));
    }

    @Operation(summary = "Найти разработчика по email (без учёта регистра)")
    @ApiResponse(responseCode = "404", description = "Разработчик не найден")
    @ApiResponse(responseCode = "400", description = "Некорректный email")
    @GetMapping("/by-email")
    public ResponseEntity<DeveloperResponse> getByEmail(
            @RequestParam @NotBlank @Email @Size(max = 254) String email) {

        return ResponseEntity.ok(developerService.getByEmail(email));
    }

    @Operation(summary = "Количество активных задач разработчика")
    @ApiResponse(responseCode = "404", description = "Разработчик не найден")
    @GetMapping("/{id}/task-count")
    public ResponseEntity<Long> getActiveTaskCount(@PathVariable Long id) {
        return ResponseEntity.ok(developerService.getActiveTaskCount(id));
    }
}
