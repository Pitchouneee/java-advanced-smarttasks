package fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.mapper;

import fr.corentinbringer.smarttasks.project.domain.model.Project;
import fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.entity.ProjectEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public Project toDomain(ProjectEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Project(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getCreatedOn()
        );
    }

    public ProjectEntity toEntity(Project domain) {
        if (domain == null) {
            return null;
        }

        ProjectEntity entity = new ProjectEntity();
        entity.setId(domain.id());
        entity.setTenantId(domain.tenantId());
        entity.setName(domain.name());
        entity.setCreatedOn(domain.createdOn());

        return entity;
    }
}
