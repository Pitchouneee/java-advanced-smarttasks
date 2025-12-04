package fr.corentinbringer.smarttasks.project.controller;

import fr.corentinbringer.smarttasks.project.model.Project;
import fr.corentinbringer.smarttasks.project.model.ProjectCreateRequest;
import fr.corentinbringer.smarttasks.project.model.ProjectListResponse;
import fr.corentinbringer.smarttasks.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public List<ProjectListResponse> findAll() {
        return projectService.findAll();
    }

    @PostMapping
    public Project create(@Valid @RequestBody ProjectCreateRequest request) {
        return projectService.create(request);
    }
}
