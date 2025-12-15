package fr.corentinbringer.smarttasks.project.infrastructure.persistence.adapter;

import fr.corentinbringer.smarttasks.project.application.port.out.AttachmentPort;
import fr.corentinbringer.smarttasks.project.domain.model.Attachment;
import fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.entity.AttachmentEntity;
import fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.mapper.AttachmentMapper;
import fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AttachmentPersistenceAdapter implements AttachmentPort {

    private final AttachmentRepository attachmentRepository;
    private final AttachmentMapper attachmentMapper;

    @Override
    public Page<Attachment> findAllByTaskIdAndTenantId(Long taskId, String tenantId, Pageable pageable) {
        Page<AttachmentEntity> entityPage = attachmentRepository.findAllByTaskIdAndTenantId(taskId, tenantId, pageable);
        return entityPage.map(attachmentMapper::toDomain);
    }

    @Override
    public Optional<Attachment> findByIdAndTenantId(Long id, String tenantId) {
        return attachmentRepository.findByIdAndTenantId(id, tenantId).map(attachmentMapper::toDomain);
    }

    @Override
    public Attachment save(Attachment attachment) {
        AttachmentEntity entity = attachmentMapper.toEntity(attachment);
        AttachmentEntity savedEntity = attachmentRepository.save(entity);
        return attachmentMapper.toDomain(savedEntity);
    }
}
