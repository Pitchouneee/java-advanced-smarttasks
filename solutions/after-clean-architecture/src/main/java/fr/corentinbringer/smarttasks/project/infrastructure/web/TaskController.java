package fr.corentinbringer.smarttasks.project.infrastructure.web;

import fr.corentinbringer.smarttasks.project.infrastructure.web.model.AttachmentResponse;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.TaskResponse;
import fr.corentinbringer.smarttasks.project.application.service.AttachmentService;
import fr.corentinbringer.smarttasks.project.application.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @GetMapping("/{id}/attachments")
    public Page<AttachmentResponse> findAttachmentsByTaskId(@PathVariable Long id, Pageable pageable) {
        return attachmentService.findAllByTaskId(id, pageable);
    }

    @PostMapping("/{id}/attachments")
    public AttachmentResponse createAttachment(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return attachmentService.create(id, file);
    }
}
