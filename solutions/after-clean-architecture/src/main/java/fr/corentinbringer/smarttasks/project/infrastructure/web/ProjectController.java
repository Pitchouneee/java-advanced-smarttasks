package fr.corentinbringer.smarttasks.project.infrastructure.web;

import fr.corentinbringer.smarttasks.project.infrastructure.web.model.ProjectCreateRequest;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.ProjectListResponse;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.ProjectResponse;
import fr.corentinbringer.smarttasks.project.application.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project operations")
public class ProjectController {

    private final ProjectService projectService;

    @Operation(
            summary = "List projects",
            description = "Retrieve a paginated list of all the user’s projects."
    )
    @GetMapping
    public Page<ProjectListResponse> findAll(Pageable pageable) {
        return projectService.findAll(pageable);
    }

    @Operation(
            summary = "Create a new project",
            description = "Create a new project for the current user.."
    )
    @PostMapping
    public ProjectResponse create(@Valid @RequestBody ProjectCreateRequest request) {
        return projectService.create(request);
    }

    @Operation(
            summary = "Get a project by ID",
            description = "Retrieve the details of a specific project."
    )
    @GetMapping("/{id}")
    public ProjectResponse findById(@Parameter(description = "Project ID to retrieve") @PathVariable Long id) {
        return projectService.findByIdResponse(id);
    }
}
