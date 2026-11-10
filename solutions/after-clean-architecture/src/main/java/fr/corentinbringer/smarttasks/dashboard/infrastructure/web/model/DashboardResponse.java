package fr.corentinbringer.smarttasks.dashboard.infrastructure.web.model;

import fr.corentinbringer.smarttasks.dashboard.application.model.DashboardSummary;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.ProjectListResponse;

import java.util.List;

public record DashboardResponse(
        long activeProjectsCount,
        long totalTasksCount,
        long overdueTasksCount,
        List<ProjectListResponse> latestProjects
) {

    public static DashboardResponse from(DashboardSummary summary) {
        return new DashboardResponse(
                summary.activeProjectsCount(),
                summary.totalTasksCount(),
                summary.overdueTasksCount(),
                summary.latestProjects().stream().map(ProjectListResponse::from).toList()
        );
    }
}
