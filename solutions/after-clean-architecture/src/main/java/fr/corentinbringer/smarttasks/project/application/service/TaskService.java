package fr.corentinbringer.smarttasks.project.application.service;

import fr.corentinbringer.smarttasks.exception.ResourceNotFoundException;
import fr.corentinbringer.smarttasks.configuration.tenant.TenantContext;
import fr.corentinbringer.smarttasks.project.application.model.CreateTaskCommand;
import fr.corentinbringer.smarttasks.project.application.model.UpdateTaskCommand;
import fr.corentinbringer.smarttasks.project.application.port.out.AttachmentPort;
import fr.corentinbringer.smarttasks.project.application.port.out.TaskPort;
import fr.corentinbringer.smarttasks.project.domain.event.AttachmentsDeletedEvent;
import fr.corentinbringer.smarttasks.project.domain.model.Project;
import fr.corentinbringer.smarttasks.project.domain.model.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskPort taskPort;
    private final ProjectService projectService;
    private final AttachmentPort attachmentPort;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<Task> findAllByProjectId(Long projectId, Pageable pageable) {
        Project project = projectService.findById(projectId);

        return taskPort.findAllByProjectIdAndTenantId(project.id(), TenantContext.getTenant(), pageable);
    }

    @Transactional(readOnly = true)
    public Task findById(Long taskId) {
        return taskPort.findByIdAndTenantId(taskId, TenantContext.getTenant())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    @Transactional
    public Task create(Long projectId, CreateTaskCommand command) {
        Project project = projectService.findById(projectId); // 404 si le projet n'est pas au tenant courant

        Task newTask = Task.create(TenantContext.getTenant(), project.id(),
                command.title(), command.description(), command.dueDate(), Instant.now());

        return taskPort.save(newTask);
    }

    @Transactional
    public Task update(Long taskId, UpdateTaskCommand command) {
        Task updated = findById(taskId)
                .withDetails(command.title(), command.description(), command.dueDate(), command.completed());

        return taskPort.save(updated);
    }

    @Transactional
    public void delete(Long taskId) {
        String tenantId = TenantContext.getTenant();
        Task task = findById(taskId);

        List<String> objectKeys = attachmentPort.findObjectKeysByTaskIdAndTenantId(task.id(), tenantId);

        taskPort.deleteById(task.id());

        eventPublisher.publishEvent(new AttachmentsDeletedEvent(objectKeys));
    }

    public long countAllTasks() {
        return taskPort.countByTenantId(TenantContext.getTenant());
    }

    public long countOverdueTasks() {
        return taskPort.countOverdueTasksByTenantId(TenantContext.getTenant(), LocalDate.now());
    }
}
