package fr.corentinbringer.smarttasks.project.infrastructure.persistence.adapter;

import fr.corentinbringer.smarttasks.project.application.port.out.ProjectPort;
import fr.corentinbringer.smarttasks.project.domain.model.Project;
import fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.entity.ProjectEntity;
import fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.mapper.ProjectMapper;
import fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProjectPersistenceAdapter implements ProjectPort {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    @Override
    public Page<Project> findAll(String tenantId, Pageable pageable) {
        Page<ProjectEntity> entityPage = projectRepository.findAllByTenantId(tenantId, pageable);
        return entityPage.map(projectMapper::toDomain);
    }

    @Override
    public Project save(Project project) {
        ProjectEntity entity = projectMapper.toEntity(project);
        ProjectEntity savedEntity = projectRepository.save(entity);
        return projectMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Project> findByIdAndTenantId(Long id, String tenantId) {
        return projectRepository.findByIdAndTenantId(id, tenantId).map(projectMapper::toDomain);
    }

    @Override
    public long countByTenantId(String tenantId) {
        return projectRepository.countByTenantId(tenantId);
    }

    @Override
    public List<Project> findLatestProjectsByTenantId(String tenantId, int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        return projectRepository.findLatestProjectsByTenantId(tenantId, pageable)
                .stream()
                .map(projectMapper::toDomain)
                .collect(Collectors.toList());
    }
}