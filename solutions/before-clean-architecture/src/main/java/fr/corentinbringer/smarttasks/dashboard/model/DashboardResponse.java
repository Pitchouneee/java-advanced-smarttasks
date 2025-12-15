package fr.corentinbringer.smarttasks.dashboard.model;

import fr.corentinbringer.smarttasks.project.model.ProjectListResponse;

import java.util.List;

public record DashboardResponse(
        long activeProjectsCount,
        long totalTasksCount,
        long overdueTasksCount,
        List<ProjectListResponse> latestProjects
) {}
