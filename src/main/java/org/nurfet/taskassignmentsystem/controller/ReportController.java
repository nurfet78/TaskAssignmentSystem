package org.nurfet.taskassignmentsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nurfet.taskassignmentsystem.dto.response.TaskStatisticsResponse;
import org.nurfet.taskassignmentsystem.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
