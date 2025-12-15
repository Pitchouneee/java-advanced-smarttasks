package fr.corentinbringer.smarttasks.dashboard.infrastructure.web;

import fr.corentinbringer.smarttasks.dashboard.infrastructure.web.model.DashboardResponse;
import fr.corentinbringer.smarttasks.dashboard.application.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse getDashboardMetrics() {
        return dashboardService.getDashboardData();
    }
}
