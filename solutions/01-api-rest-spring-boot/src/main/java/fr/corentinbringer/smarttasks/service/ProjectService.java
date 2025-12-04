package fr.corentinbringer.smarttasks.service;

import fr.corentinbringer.smarttasks.model.Project;
import fr.corentinbringer.smarttasks.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public List<Project> findAll() {
        return projectRepository.findAll();
    }

    public Project create(String name) {
        return projectRepository.save(new Project(null, name));
    }
}
