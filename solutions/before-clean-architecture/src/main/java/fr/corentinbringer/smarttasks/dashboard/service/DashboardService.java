package fr.corentinbringer.smarttasks.dashboard.service;

import fr.corentinbringer.smarttasks.dashboard.model.DashboardResponse;
import fr.corentinbringer.smarttasks.project.service.ProjectService;
import fr.corentinbringer.smarttasks.project.service.TaskService;
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
