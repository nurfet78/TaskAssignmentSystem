package org.nurfet.taskassignmentsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nurfet.taskassignmentsystem.dto.response.StatusDurationResponse;
import org.nurfet.taskassignmentsystem.dto.response.TaskStatisticsResponse;
import org.nurfet.taskassignmentsystem.dto.response.UnassignableTaskResponse;
import org.nurfet.taskassignmentsystem.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports")
@Validated
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "Статистика задач",
            description = "Распределение задач по статусам в штуках и процентах и время выполнения "
                    + "(создание -> завершение) по приоритетам для задач в статусе DONE.")
    @GetMapping("/tasks/statistics")
    public ResponseEntity<TaskStatisticsResponse> getTaskStatistics() {
        return ResponseEntity.ok(reportService.getTaskStatistics());
    }

    @Operation(summary = "Задачи без подходящего исполнителя",
            description = "Задачи NEW без исполнителя и с навыками, для которых нет ни одного активного "
                    + "разработчика хотя бы с одним требуемым навыком. Для них автоназначение вернёт 409. "
                    + "Список целиком, по приоритету.")
    @GetMapping("/tasks/unassignable")
    public ResponseEntity<List<UnassignableTaskResponse>> getUnassignableTasks() {
        return ResponseEntity.ok(reportService.getUnassignableTasks());
    }

    @Operation(summary = "Время задачи в каждом статусе",
            description = "Суммируется по всем заходам в статус (IN_PROGRESS -> NEW -> IN_PROGRESS). "
                    + "Смена исполнителя без смены статуса не создаёт нового интервала. "
                    + "Для текущего статуса время считается до момента запроса, "
                    + "для конечных (DONE, CANCELLED) не считается.")
    @ApiResponse(responseCode = "404", description = "Задача не найдена")
    @GetMapping("/tasks/{id}/status-time")
    public ResponseEntity<List<StatusDurationResponse>> getStatusDurations(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getStatusDurations(id));
    }
}
