package fr.corentinbringer.smarttasks.project.application.service;

import fr.corentinbringer.smarttasks.configuration.tenant.TenantContext;
import fr.corentinbringer.smarttasks.project.application.port.out.TaskPort;
import fr.corentinbringer.smarttasks.project.domain.model.Project;
import fr.corentinbringer.smarttasks.project.domain.model.Task;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.TaskCreateRequest;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.TaskListResponse;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.TaskResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskPort taskPort;
    private final ProjectService projectService;

    private TaskResponse mapToResponse(Task task) {
        return new TaskResponse(
                task.id(),
                task.title(),
                task.description(),
                task.dueDate(),
                task.completed(),
                task.projectId(),
                task.createdOn()
        );
    }

    private TaskListResponse mapToListResponse(Task task) {
        return new TaskListResponse(
                task.id(),
                task.title(),
                task.description(),
                task.dueDate(),
                task.completed()
        );
    }

    @Transactional
    public Page<TaskListResponse> findAllByProjectId(Long projectId, Pageable pageable) {
        Project project = projectService.findById(projectId);

        return taskPort.findAllByProjectIdAndTenantId(project.id(), TenantContext.getTenant(), pageable)
                .map(this::mapToListResponse);
    }

    public Task findById(Long taskId) {
        String tenantId = TenantContext.getTenant();
        return taskPort.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Task not found"));
    }

    @Transactional
    public TaskResponse create(Long projectId, TaskCreateRequest request) {
        String tenantId = TenantContext.getTenant();
        Project project = projectService.findById(projectId);

        Task newTask = new Task(
                null,
                tenantId,
                project.id(),
                request.title(),
                request.description(),
                request.dueDate(),
                false,
                LocalDateTime.now()
        );

        Task savedTask = taskPort.save(newTask);

        return mapToResponse(savedTask);
    }

    @Transactional
    public TaskResponse findByIdResponse(Long taskId) {
        Task task = findById(taskId);
        return mapToResponse(task);
    }

    public long countAllTasks() {
        return taskPort.countByTenantId(TenantContext.getTenant());
    }

    public long countOverdueTasks() {
        return taskPort.countOverdueTasksByTenantId(TenantContext.getTenant(), LocalDate.now());
    }
}
