package fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.mapper;

import fr.corentinbringer.smarttasks.project.domain.model.Task;
import fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.entity.ProjectEntity;
import fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.entity.TaskEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public Task toDomain(TaskEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Task(
                entity.getId(),
                entity.getTenantId(),
                entity.getProject().getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getDueDate(),
                entity.isCompleted(),
                entity.getCreatedOn()
        );
    }

    public TaskEntity toEntity(Task domain) {
        if (domain == null) {
            return null;
        }

        TaskEntity entity = new TaskEntity();
        entity.setId(domain.id());
        entity.setTenantId(domain.tenantId());

        ProjectEntity projectRef = new ProjectEntity();
        projectRef.setId(domain.projectId());
        entity.setProject(projectRef);

        entity.setTitle(domain.title());
        entity.setDescription(domain.description());
        entity.setDueDate(domain.dueDate());
        entity.setCompleted(domain.completed());
        entity.setCreatedOn(domain.createdOn());

        return entity;
    }
}
