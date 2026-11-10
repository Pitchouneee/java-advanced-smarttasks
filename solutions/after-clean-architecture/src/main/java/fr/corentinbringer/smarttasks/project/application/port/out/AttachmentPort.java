package fr.corentinbringer.smarttasks.project.application.port.out;

import fr.corentinbringer.smarttasks.project.domain.model.Attachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AttachmentPort {

    Page<Attachment> findAllByTaskIdAndTenantId(Long taskId, String tenantId, Pageable pageable);

    Optional<Attachment> findByIdAndTenantId(Long id, String tenantId);

    Attachment save(Attachment attachment);

    void deleteById(Long id);

    List<String> findObjectKeysByTaskIdAndTenantId(Long taskId, String tenantId);

    List<String> findObjectKeysByProjectIdAndTenantId(Long projectId, String tenantId);
}