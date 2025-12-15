package fr.corentinbringer.smarttasks.project.application.port.out;

import fr.corentinbringer.smarttasks.project.domain.model.Attachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface AttachmentPort {

    Page<Attachment> findAllByTaskIdAndTenantId(Long taskId, String tenantId, Pageable pageable);

    Optional<Attachment> findByIdAndTenantId(Long id, String tenantId);

    Attachment save(Attachment attachment);
}