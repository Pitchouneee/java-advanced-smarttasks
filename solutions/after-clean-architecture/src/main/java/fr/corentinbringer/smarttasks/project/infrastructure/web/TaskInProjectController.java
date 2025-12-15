package fr.corentinbringer.smarttasks.project.infrastructure.web;

import fr.corentinbringer.smarttasks.project.infrastructure.web.model.TaskCreateRequest;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.TaskListResponse;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.TaskResponse;
import fr.corentinbringer.smarttasks.project.application.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks in Project", description = "Task operations within a project")
public class TaskInProjectController {

    private final TaskService taskService;

    @Operation(
            summary = "List a project’s tasks",
            description = "Retrieve a paginated list of tasks for a specific project."
    )
    @GetMapping
    public Page<TaskListResponse> findAll(@Parameter(description = "Project ID") @PathVariable Long projectId, Pageable pageable) {
        return taskService.findAllByProjectId(projectId, pageable);
    }

    @Operation(
            summary = "Create a new task in a project",
            description = "“Create a new task associated with a specific project."
    )
    @PostMapping
    public TaskResponse create(@Parameter(description = "Project ID") @PathVariable Long projectId, @Valid @RequestBody TaskCreateRequest request) {
        return taskService.create(projectId, request);
    }
}
