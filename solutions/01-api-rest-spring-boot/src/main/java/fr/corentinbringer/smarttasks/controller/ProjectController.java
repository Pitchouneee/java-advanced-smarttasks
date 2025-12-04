package fr.corentinbringer.smarttasks.controller;

import fr.corentinbringer.smarttasks.model.Project;
import fr.corentinbringer.smarttasks.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public List<Project> findAll() {
        return projectService.findAll();
    }

    @PostMapping
    public Project create(@RequestBody Map<String, String> body) {
        return projectService.create(body.get("name"));
    }
}
