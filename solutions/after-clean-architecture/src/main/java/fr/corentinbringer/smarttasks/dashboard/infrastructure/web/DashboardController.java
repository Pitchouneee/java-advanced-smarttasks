package fr.corentinbringer.smarttasks.dashboard.infrastructure.web;

import fr.corentinbringer.smarttasks.dashboard.infrastructure.web.model.DashboardResponse;
import fr.corentinbringer.smarttasks.dashboard.application.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard operations")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "Get dashboard metrics",
            description = "Retrieve the total number of active projects, total tasks, and overdue tasks, as well as the latest projects."
    )
    @GetMapping
    public DashboardResponse getDashboardMetrics() {
        return dashboardService.getDashboardData();
    }
}
