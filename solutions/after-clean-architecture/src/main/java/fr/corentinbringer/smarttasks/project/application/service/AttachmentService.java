package fr.corentinbringer.smarttasks.project.application.service;

import fr.corentinbringer.smarttasks.configuration.tenant.TenantContext;
import fr.corentinbringer.smarttasks.project.application.port.out.AttachmentPort;
import fr.corentinbringer.smarttasks.project.application.port.out.FileStoragePort;
import fr.corentinbringer.smarttasks.project.domain.model.Attachment;
import fr.corentinbringer.smarttasks.project.domain.model.Task;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.AttachmentResponse;
import fr.corentinbringer.smarttasks.project.infrastructure.web.model.DownloadResult;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentPort attachmentPort;
    private final TaskService taskService;

    private final FileStoragePort fileStoragePort;

    private AttachmentResponse mapToResponse(Attachment attachment) {
        return new AttachmentResponse(
                attachment.id(),
                attachment.originalName(),
                attachment.mimeType(),
                attachment.size(),
                "/api/attachments/" + attachment.id() + "/download"
        );
    }

    @Transactional
    public Page<AttachmentResponse> findAllByTaskId(Long taskId, Pageable pageable) {
        Task task = taskService.findById(taskId);

        return attachmentPort.findAllByTaskIdAndTenantId(task.id(), TenantContext.getTenant(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public AttachmentResponse create(Long taskId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new NoSuchElementException("File is empty");
        }

        String tenantId = TenantContext.getTenant();
        Task task = taskService.findById(taskId);

        try {
            String objectKey = fileStoragePort.uploadFile(file);

            Attachment attachment = new Attachment(
                    null,
                    tenantId,
                    objectKey,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    LocalDateTime.now(),
                    task.id()
            );

            Attachment savedAttachment = attachmentPort.save(attachment);

            return mapToResponse(savedAttachment);

        } catch (Exception e) {
            throw new NoSuchElementException("Attachment upload failed", e);
        }
    }

    public DownloadResult download(Long attachmentId) {
        String tenantId = TenantContext.getTenant();

        Attachment attachment = attachmentPort.findByIdAndTenantId(attachmentId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Attachment not found"));

        try {
            InputStream is = fileStoragePort.downloadFile(attachment.objectKey());

            return new DownloadResult(
                    new InputStreamResource(is),
                    attachment.originalName(),
                    attachment.mimeType(),
                    attachment.size()
            );
        } catch (Exception e) {
            throw new NoSuchElementException("Attachment download failed", e);
        }
    }
}