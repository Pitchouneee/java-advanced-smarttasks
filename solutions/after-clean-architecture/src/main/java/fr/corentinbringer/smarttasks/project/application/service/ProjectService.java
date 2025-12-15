package fr.corentinbringer.smarttasks.project.application.service;

import fr.corentinbringer.smarttasks.configuration.tenant.TenantContext;
import fr.corentinbringer.smarttasks.project.application.port.out.ProjectPort;
import fr.corentinbringer.smarttasks.project.domain.model.Project;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.ProjectCreateRequest;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.ProjectListResponse;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.ProjectResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectPort projectPort;

    private ProjectResponse mapToResponse(Project project) {
        return new ProjectResponse(project.id(), project.name(), project.createdOn());
    }

    private ProjectListResponse mapToListResponse(Project project) {
        return new ProjectListResponse(project.id(), project.name(), project.createdOn());
    }

    @Transactional
    public Page<ProjectListResponse> findAll(Pageable pageable) {
        String tenantId = TenantContext.getTenant();
        return projectPort.findAll(tenantId, pageable).map(this::mapToListResponse);
    }

    @Transactional
    public ProjectResponse create(ProjectCreateRequest request) {
        String tenantId = TenantContext.getTenant();

        Project newProject = new Project(null, tenantId, request.name(), LocalDateTime.now());

        Project savedProject = projectPort.save(newProject);

        return mapToResponse(savedProject);
    }

    public Project findById(Long projectId) {
        return projectPort.findByIdAndTenantId(projectId, TenantContext.getTenant())
                .orElseThrow(() -> new NoSuchElementException("Project not found"));
    }

    public ProjectResponse findByIdResponse(Long projectId) {
        Project project = findById(projectId);
        return mapToResponse(project);
    }

    public long countAllProjects() {
        return projectPort.countByTenantId(TenantContext.getTenant());
    }

    public List<ProjectListResponse> findLatestProjects(int limit) {
        String tenantId = TenantContext.getTenant();
        return projectPort.findLatestProjectsByTenantId(tenantId, limit)
                .stream()
                .map(this::mapToListResponse)
                .collect(Collectors.toList());
    }
}