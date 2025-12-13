package fr.corentinbringer.smarttasks.project.controller;

import fr.corentinbringer.smarttasks.project.model.TaskCreateRequest;
import fr.corentinbringer.smarttasks.project.model.TaskListResponse;
import fr.corentinbringer.smarttasks.project.model.TaskResponse;
import fr.corentinbringer.smarttasks.project.service.TaskService;
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
