package fr.corentinbringer.smarttasks.project.controller;

import fr.corentinbringer.smarttasks.project.model.AttachmentResponse;
import fr.corentinbringer.smarttasks.project.model.TaskResponse;
import fr.corentinbringer.smarttasks.project.model.TaskUpdateRequest;
import fr.corentinbringer.smarttasks.project.service.AttachmentService;
import fr.corentinbringer.smarttasks.project.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final AttachmentService attachmentService;

    @GetMapping("/{id}")
    public TaskResponse findById(@PathVariable Long id) {
        return taskService.findByIdResponse(id);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        return taskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }

    @GetMapping("/{id}/attachments")
    public Page<AttachmentResponse> findAttachmentsByTaskId(@PathVariable Long id, Pageable pageable) {
        return attachmentService.findAllByTaskId(id, pageable);
    }

    @PostMapping("/{id}/attachments")

    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse createAttachment(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return attachmentService.create(id, file);
    }
}
