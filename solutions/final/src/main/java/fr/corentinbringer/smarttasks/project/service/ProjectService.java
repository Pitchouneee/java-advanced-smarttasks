package fr.corentinbringer.smarttasks.project.service;

import fr.corentinbringer.smarttasks.configuration.tenant.TenantContext;
import fr.corentinbringer.smarttasks.project.model.Project;
import fr.corentinbringer.smarttasks.project.model.ProjectCreateRequest;
import fr.corentinbringer.smarttasks.project.model.ProjectListResponse;
import fr.corentinbringer.smarttasks.project.model.ProjectResponse;
import fr.corentinbringer.smarttasks.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public List<ProjectListResponse> findAll() {
        String tenantId = TenantContext.getTenant();
        return projectRepository.findAllListByTenantId(tenantId);
    }

    public ProjectResponse create(ProjectCreateRequest request) {
        Project project = new Project();
        project.setName(request.name());
        project.setTenantId(TenantContext.getTenant());
        Project savedProject = projectRepository.save(project);

        return new ProjectResponse(
                savedProject.getId(),
                savedProject.getName(),
                savedProject.getCreatedOn()
        );
    }

    public Project findById(Long projectId) {
        return projectRepository.findByIdAndTenantId(projectId, TenantContext.getTenant())
                .orElseThrow(() -> new NoSuchElementException("Project not found"));
    }

    public ProjectResponse findByIdResponse(Long projectId) {
        Project project = findById(projectId);

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getCreatedOn()
        );
    }
}
