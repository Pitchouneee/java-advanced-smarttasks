package fr.corentinbringer.smarttasks.project.service;

import fr.corentinbringer.smarttasks.exception.ResourceNotFoundException;
import fr.corentinbringer.smarttasks.configuration.tenant.TenantContext;
import fr.corentinbringer.smarttasks.project.event.AttachmentsDeletedEvent;
import fr.corentinbringer.smarttasks.project.model.*;
import fr.corentinbringer.smarttasks.project.repository.AttachmentRepository;
import fr.corentinbringer.smarttasks.project.repository.TaskRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final AttachmentRepository attachmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    private TaskResponse toResponse(Task task) {
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

    @Transactional
    public Page<TaskListResponse> findAllByProjectId(Long projectId, Pageable pageable) {
        Project project = projectService.findById(projectId);

        return taskRepository.findAllByProjectIdAndTenantId(project.getId(), TenantContext.getTenant(), pageable);
    }

    public Task findById(Long taskId) {
        String tenantId = TenantContext.getTenant();
        return taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
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

        return toResponse(savedTask);
    }

    @Transactional
    public TaskResponse findByIdResponse(Long taskId) {
        Task task = findById(taskId);

        return toResponse(task);
    }

    @Transactional
    public TaskResponse update(Long taskId, TaskUpdateRequest request) {
        Task task = findById(taskId);

        // Dirty checking : pas besoin de save(), l'UPDATE est fait au commit.
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        task.setCompleted(request.completed());

        return toResponse(task);
    }

    @Transactional
    public void delete(Long taskId) {
        String tenantId = TenantContext.getTenant();
        Task task = findById(taskId);

        List<String> objectKeys = attachmentRepository.findObjectKeysByTaskIdAndTenantId(task.getId(), tenantId);

        // cascade = ALL : supprime aussi les pièces jointes en base.
        taskRepository.delete(task);

        eventPublisher.publishEvent(new AttachmentsDeletedEvent(objectKeys));
    }

    public long countAllTasks() {
        return taskRepository.countByTenantId(TenantContext.getTenant());
    }

    public long countOverdueTasks() {
        return taskRepository.countOverdueTasksByTenantId(TenantContext.getTenant(), LocalDate.now());
    }
}
