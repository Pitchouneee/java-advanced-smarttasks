package fr.corentinbringer.smarttasks.project.infrastructure.web;

import fr.corentinbringer.smarttasks.project.infrastructure.web.model.TaskCreateRequest;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.TaskListResponse;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.TaskResponse;
import fr.corentinbringer.smarttasks.project.application.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class TaskInProjectController {

    private final TaskService taskService;

    @GetMapping
    public Page<TaskListResponse> findAll(@PathVariable Long projectId, Pageable pageable) {
        return taskService.findAllByProjectId(projectId, pageable);
    }

    @PostMapping
    public TaskResponse create(@PathVariable Long projectId, @Valid @RequestBody TaskCreateRequest request) {
        return taskService.create(projectId, request);
    }
}
