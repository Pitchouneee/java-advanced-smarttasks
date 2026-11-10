package fr.corentinbringer.smarttasks.dashboard.application.service;

import fr.corentinbringer.smarttasks.dashboard.application.model.DashboardSummary;
import fr.corentinbringer.smarttasks.project.application.service.ProjectService;
import fr.corentinbringer.smarttasks.project.application.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int LATEST_PROJECTS_LIMIT = 5;

    private final ProjectService projectService;
    private final TaskService taskService;

    @Transactional(readOnly = true)
    public DashboardSummary getSummary() {
        return new DashboardSummary(
                projectService.countActiveProjects(),
                taskService.countAllTasks(),
                taskService.countOverdueTasks(),
                projectService.findLatestProjects(LATEST_PROJECTS_LIMIT)
        );
    }
}
