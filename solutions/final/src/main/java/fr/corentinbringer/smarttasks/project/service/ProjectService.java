package fr.corentinbringer.smarttasks.project.service;

import fr.corentinbringer.smarttasks.project.model.Project;
import fr.corentinbringer.smarttasks.project.model.ProjectCreateRequest;
import fr.corentinbringer.smarttasks.project.model.ProjectListResponse;
import fr.corentinbringer.smarttasks.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public List<ProjectListResponse> findAll() {
        return projectRepository.findAllList();
    }

    public Project create(ProjectCreateRequest request) {
        Project project = new Project();
        project.setName(request.name());
        return projectRepository.save(project);
    }
}
