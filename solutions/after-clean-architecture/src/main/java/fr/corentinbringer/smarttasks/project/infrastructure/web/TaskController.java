package fr.corentinbringer.smarttasks.project.infrastructure.web;

import fr.corentinbringer.smarttasks.project.infrastructure.web.model.AttachmentResponse;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.TaskResponse;
import fr.corentinbringer.smarttasks.project.application.service.AttachmentService;
import fr.corentinbringer.smarttasks.project.application.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
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
        return taskService.findByIdResponse(id);
    }

    @Operation(
            summary = "List a task’s attachments",
            description = "Retrieve a paginated list of attachments for a specific task."
    )
    @GetMapping("/{id}/attachments")
    public Page<AttachmentResponse> findAttachmentsByTaskId(@Parameter(description = "Task ID") @PathVariable Long id, Pageable pageable) {
        return attachmentService.findAllByTaskId(id, pageable);
    }

    @Operation(
            summary = "Add an attachment to a task",
            description = "Upload a file and associate it with a task.")
    @PostMapping(value = "/{id}/attachments", consumes = {"multipart/form-data"})
    public AttachmentResponse createAttachment(
            @Parameter(description = "Task ID") @PathVariable Long id,
            @Parameter(description = "File to upload", required = true) @RequestParam("file") MultipartFile file) {
        return attachmentService.create(id, file);
    }
}
