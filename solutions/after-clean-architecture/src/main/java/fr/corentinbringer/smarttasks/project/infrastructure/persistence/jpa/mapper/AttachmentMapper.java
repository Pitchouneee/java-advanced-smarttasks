package fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.mapper;

import fr.corentinbringer.smarttasks.project.domain.model.Attachment;
import fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.entity.AttachmentEntity;
import fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.entity.TaskEntity;
import org.springframework.stereotype.Component;

@Component
public class AttachmentMapper {

    public Attachment toDomain(AttachmentEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Attachment(
                entity.getId(),
                entity.getTenantId(),
                entity.getObjectKey(),
                entity.getOriginalName(),
                entity.getMimeType(),
                entity.getSize(),
                entity.getUploadedOn(),
                entity.getTask().getId()
        );
    }

    public AttachmentEntity toEntity(Attachment domain) {
        if (domain == null) {
            return null;
        }

        AttachmentEntity entity = new AttachmentEntity();
        entity.setId(domain.id());
        entity.setTenantId(domain.tenantId());
        entity.setObjectKey(domain.objectKey());
        entity.setOriginalName(domain.originalName());
        entity.setMimeType(domain.mimeType());
        entity.setSize(domain.size());
        entity.setUploadedOn(domain.uploadedOn());

        TaskEntity taskRef = new TaskEntity();
        taskRef.setId(domain.taskId());
        entity.setTask(taskRef);

        return entity;
    }
}
