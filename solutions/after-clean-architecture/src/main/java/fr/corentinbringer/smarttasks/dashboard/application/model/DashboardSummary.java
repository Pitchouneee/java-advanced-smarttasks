package fr.corentinbringer.smarttasks.dashboard.application.model;

import fr.corentinbringer.smarttasks.project.domain.model.Project;

import java.util.List;

/** Indicateurs du tableau de bord, indépendants du format JSON de l'API. */
public record DashboardSummary(
        long activeProjectsCount,
        long totalTasksCount,
        long overdueTasksCount,
        List<Project> latestProjects
) {}
