package fr.corentinbringer.smarttasks.project.infrastructure.web;

import fr.corentinbringer.smarttasks.project.infrastructure.web.model.ProjectCreateRequest;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.ProjectListResponse;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.ProjectResponse;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.ProjectUpdateRequest;
import fr.corentinbringer.smarttasks.project.application.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
        return projectService.findAll(pageable).map(ProjectListResponse::from);
    }

    @Operation(
            summary = "Create a new project",
            description = "Create a new project for the current user."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody ProjectCreateRequest request) {
        return ProjectResponse.from(projectService.create(request.name()));
    }

    @Operation(
            summary = "Get a project by ID",
            description = "Retrieve the details of a specific project."
    )
    @GetMapping("/{id}")
    public ProjectResponse findById(@Parameter(description = "Project ID to retrieve") @PathVariable Long id) {
        return ProjectResponse.from(projectService.findById(id));
    }

    @Operation(
            summary = "Rename a project",
            description = "Update the name of a project owned by the current user."
    )
    @PutMapping("/{id}")
    public ProjectResponse update(@Parameter(description = "Project ID to update") @PathVariable Long id,
                                  @Valid @RequestBody ProjectUpdateRequest request) {
        return ProjectResponse.from(projectService.rename(id, request.name()));
    }

    @Operation(
            summary = "Delete a project",
            description = "Delete a project, its tasks and their attachments (database and object storage)."
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Project ID to delete") @PathVariable Long id) {
        projectService.delete(id);
    }
}
