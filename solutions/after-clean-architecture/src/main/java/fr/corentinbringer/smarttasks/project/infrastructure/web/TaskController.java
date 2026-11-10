package fr.corentinbringer.smarttasks.project.infrastructure.web;

import fr.corentinbringer.smarttasks.project.infrastructure.web.model.AttachmentResponse;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.TaskResponse;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.TaskUpdateRequest;
import fr.corentinbringer.smarttasks.project.application.service.AttachmentService;
import fr.corentinbringer.smarttasks.project.application.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import fr.corentinbringer.smarttasks.exception.StorageException;
import fr.corentinbringer.smarttasks.project.application.model.FileContent;
import fr.corentinbringer.smarttasks.project.application.model.UpdateTaskCommand;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Individual task operations")
public class TaskController {

    private final TaskService taskService;
    private final AttachmentService attachmentService;

    @Operation(
            summary = "Get a task by ID",
            description = "Retrieve the details of a specific task."
    )
    @GetMapping("/{id}")
    public TaskResponse findById(@Parameter(description = "Task ID to retrieve") @PathVariable Long id) {
        return TaskResponse.from(taskService.findById(id));
    }

    @Operation(
            summary = "Update a task",
            description = "Replace the editable fields of a task (title, description, due date, completed)."
    )
    @PutMapping("/{id}")
    public TaskResponse update(@Parameter(description = "Task ID to update") @PathVariable Long id,
                               @Valid @RequestBody TaskUpdateRequest request) {
        UpdateTaskCommand command = new UpdateTaskCommand(
                request.title(), request.description(), request.dueDate(), request.completed());
        return TaskResponse.from(taskService.update(id, command));
    }

    @Operation(
            summary = "Delete a task",
            description = "Delete a task and its attachments (database and object storage)."
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Task ID to delete") @PathVariable Long id) {
        taskService.delete(id);
    }

    @Operation(
            summary = "List a task’s attachments",
            description = "Retrieve a paginated list of attachments for a specific task."
    )
    @GetMapping("/{id}/attachments")
    public Page<AttachmentResponse> findAttachmentsByTaskId(@Parameter(description = "Task ID") @PathVariable Long id, Pageable pageable) {
        return attachmentService.findAllByTaskId(id, pageable).map(AttachmentResponse::from);
    }

    @Operation(
            summary = "Add an attachment to a task",
            description = "Upload a file and associate it with a task.")
    @PostMapping(value = "/{id}/attachments", consumes = {"multipart/form-data"})
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse createAttachment(
            @Parameter(description = "Task ID") @PathVariable Long id,
            @Parameter(description = "File to upload", required = true) @RequestParam("file") MultipartFile file) {
        // Traduction HTTP → application : MultipartFile s'arrête ici.
        try (InputStream content = file.getInputStream()) {
            FileContent fileContent = new FileContent(
                    file.getOriginalFilename(), file.getContentType(), file.getSize(), content);
            return AttachmentResponse.from(attachmentService.create(id, fileContent));
        } catch (IOException e) {
            throw new StorageException("Cannot read the uploaded file", e);
        }
    }
}
