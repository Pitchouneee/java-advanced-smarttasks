package fr.corentinbringer.smarttasks.dashboard.infrastructure.web.model;

import fr.corentinbringer.smarttasks.project.infrastructure.web.model.ProjectListResponse;

import java.util.List;

public record DashboardResponse(
        long activeProjectsCount,
        long totalTasksCount,
        long overdueTasksCount,
        List<ProjectListResponse> latestProjects
) {}
