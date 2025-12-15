package fr.corentinbringer.smarttasks.project.service;

import fr.corentinbringer.smarttasks.configuration.tenant.TenantContext;
import fr.corentinbringer.smarttasks.project.model.*;
import fr.corentinbringer.smarttasks.project.repository.TaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectService projectService;

    @Transactional
    public Page<TaskListResponse> findAllByProjectId(Long projectId, Pageable pageable) {
        Project project = projectService.findById(projectId);

        return taskRepository.findAllByProjectIdAndTenantId(project.getId(), TenantContext.getTenant(), pageable);
    }

    public Task findById(Long taskId) {
        String tenantId = TenantContext.getTenant();
        return taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Task not found"));
    }

    @Transactional
    public TaskResponse create(Long projectId, TaskCreateRequest request) {
        String tenantId = TenantContext.getTenant();
        Project project = projectService.findById(projectId);

        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        task.setProject(project);
        task.setTenantId(tenantId);

        Task savedTask = taskRepository.save(task);

        return new TaskResponse(
                savedTask.getId(),
                savedTask.getTitle(),
                savedTask.getDescription(),
                savedTask.getDueDate(),
                savedTask.isCompleted(),
                savedTask.getProject().getId(),
                savedTask.getCreatedOn()
        );
    }

    @Transactional
    public TaskResponse findByIdResponse(Long taskId) {
        Task task = findById(taskId);

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDueDate(),
                task.isCompleted(),
                task.getProject().getId(),
                task.getCreatedOn()
        );
    }

    public long countAllTasks() {
        return taskRepository.countByTenantId(TenantContext.getTenant());
    }

    public long countOverdueTasks() {
        return taskRepository.countOverdueTasksByTenantId(TenantContext.getTenant(), LocalDate.now());
    }
}
