package org.nurfet.taskassignmentsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.nurfet.taskassignmentsystem.dto.request.CreateSkillRequest;
import org.nurfet.taskassignmentsystem.dto.request.UpdateSkillRequest;
import org.nurfet.taskassignmentsystem.dto.response.PageResponse;
import org.nurfet.taskassignmentsystem.dto.response.SkillResponse;
import org.nurfet.taskassignmentsystem.service.SkillService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/skills")
@RequiredArgsConstructor
@Tag(name = "Skills")
@Validated
public class SkillController {

    private final SkillService skillService;

    @Operation(summary = "Создание навыка")
    @ApiResponse(responseCode = "201", description = "Навык создан")
    @ApiResponse(responseCode = "409", description = "Навык с таким именем уже существует")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    @PostMapping
    public ResponseEntity<SkillResponse> create(@Valid @RequestBody CreateSkillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(skillService.create(request));
    }

    @Operation(summary = "Частичное обновление навыка",
            description = "Поля со значением null не изменяются.")
    @ApiResponse(responseCode = "409", description = "Навык с таким именем уже существует")
    @ApiResponse(responseCode = "404", description = "Навык не найден")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    @PatchMapping("/{id}")
    public ResponseEntity<SkillResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody UpdateSkillRequest request) {

        return ResponseEntity.ok(skillService.update(id, request));
    }

    @Operation(summary = "Получить навык по ID")
    @ApiResponse(responseCode = "404", description = "Навык не найден")
    @GetMapping("/{id}")
    public ResponseEntity<SkillResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(skillService.getById(id));
    }

    @Operation(summary = "Найти навык по имени (без учёта регистра)")
    @ApiResponse(responseCode = "404", description = "Навык не найден")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    @GetMapping("/by-name")
    public ResponseEntity<SkillResponse> getByName(
            @RequestParam @NotBlank @Size(max = 100) String name) {

        return ResponseEntity.ok(skillService.getByName(name));
    }

    @Operation(summary = "Получить все навыки")
    @GetMapping
    public ResponseEntity<PageResponse<SkillResponse>> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {

        return ResponseEntity.ok(skillService.getAll(pageable));
    }

    @Operation(summary = "Удаление навыка",
            description = "Навык нельзя удалить, пока он назначен разработчику или требуется задаче.")
    @ApiResponse(responseCode = "204", description = "Навык удалён")
    @ApiResponse(responseCode = "409", description = "Навык используется")
    @ApiResponse(responseCode = "404", description = "Навык не найден")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        skillService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
