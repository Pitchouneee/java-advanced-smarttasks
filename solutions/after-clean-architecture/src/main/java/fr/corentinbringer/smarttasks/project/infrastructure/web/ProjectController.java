package fr.corentinbringer.smarttasks.project.infrastructure.web;

import fr.corentinbringer.smarttasks.project.infrastructure.web.model.ProjectCreateRequest;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.ProjectListResponse;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.ProjectResponse;
import fr.corentinbringer.smarttasks.project.application.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public Page<ProjectListResponse> findAll(Pageable pageable) {
        return projectService.findAll(pageable);
    }

    @PostMapping
    public ProjectResponse create(@Valid @RequestBody ProjectCreateRequest request) {
        return projectService.create(request);
    }

    @GetMapping("/{id}")
    public ProjectResponse findById(@PathVariable Long id) {
        return projectService.findByIdResponse(id);
    }
}
