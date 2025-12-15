package fr.corentinbringer.smarttasks.project.application.port.out;

import fr.corentinbringer.smarttasks.project.domain.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProjectPort {

    Page<Project> findAll(String tenantId, Pageable pageable);

    Project save(Project project);

    Optional<Project> findByIdAndTenantId(Long id, String tenantId);

    long countByTenantId(String tenantId);

    List<Project> findLatestProjectsByTenantId(String tenantId, int limit);
}