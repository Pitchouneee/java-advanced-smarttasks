package fr.corentinbringer.smarttasks.project.controller;

import fr.corentinbringer.smarttasks.project.model.AttachmentResponse;
import fr.corentinbringer.smarttasks.project.model.TaskResponse;
import fr.corentinbringer.smarttasks.project.service.AttachmentService;
import fr.corentinbringer.smarttasks.project.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    public List<AttachmentResponse> findAttachmentsByTaskId(@PathVariable Long id) {
        return attachmentService.findAllByTaskId(id);
    }

    @PostMapping("/{id}/attachments")
    public AttachmentResponse createAttachment(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return attachmentService.create(id, file);
    }
}
