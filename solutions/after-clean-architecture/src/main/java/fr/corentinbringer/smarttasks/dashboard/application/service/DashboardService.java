package fr.corentinbringer.smarttasks.dashboard.application.service;

import fr.corentinbringer.smarttasks.dashboard.infrastructure.web.model.DashboardResponse;
import fr.corentinbringer.smarttasks.project.application.service.ProjectService;
import fr.corentinbringer.smarttasks.project.application.service.TaskService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProjectService projectService;
    private final TaskService taskService;

    @Transactional
    public DashboardResponse getDashboardData() {
        final int LATEST_PROJECTS_LIMIT = 5;

        long activeProjectsCount = projectService.countAllProjects();
        long totalTasksCount = taskService.countAllTasks();
        long overdueTasksCount = taskService.countOverdueTasks();

        var latestProjects = projectService.findLatestProjects(LATEST_PROJECTS_LIMIT);

        return new DashboardResponse(
                activeProjectsCount,
                totalTasksCount,
                overdueTasksCount,
                latestProjects
        );
    }
}
