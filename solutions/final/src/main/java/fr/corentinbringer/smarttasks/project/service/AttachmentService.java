package fr.corentinbringer.smarttasks.project.service;

import fr.corentinbringer.smarttasks.configuration.tenant.TenantContext;
import fr.corentinbringer.smarttasks.minio.service.MinioService;
import fr.corentinbringer.smarttasks.project.model.Attachment;
import fr.corentinbringer.smarttasks.project.model.AttachmentResponse;
import fr.corentinbringer.smarttasks.project.model.DownloadResult;
import fr.corentinbringer.smarttasks.project.model.Task;
import fr.corentinbringer.smarttasks.project.repository.AttachmentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskService taskService;
    private final MinioService minioService;

    @Transactional
    public Page<AttachmentResponse> findAllByTaskId(Long taskId, Pageable pageable) {
        Task task = taskService.findById(taskId);

        return attachmentRepository.findAllByTaskIdAndTenantId(task.getId(), TenantContext.getTenant(), pageable);
    }

    @Transactional
    public AttachmentResponse create(Long taskId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new NoSuchElementException("File is empty");
        }

        String tenantId = TenantContext.getTenant();
        Task task = taskService.findById(taskId);

        try {
            String objectKey = minioService.uploadFile(file);

            Attachment attachment = new Attachment();
            attachment.setTenantId(tenantId);
            attachment.setTask(task);
            attachment.setObjectKey(objectKey);
            attachment.setOriginalName(file.getOriginalFilename());
            attachment.setMimeType(file.getContentType());
            attachment.setSize(file.getSize());

            Attachment savedAttachment = attachmentRepository.save(attachment);

            return new AttachmentResponse(
                    savedAttachment.getId(),
                    savedAttachment.getOriginalName(),
                    savedAttachment.getMimeType(),
                    savedAttachment.getSize(),
                    "/api/attachments/" + savedAttachment.getId() + "/download"
            );

        } catch (Exception e) {
            throw new NoSuchElementException("Attachment upload failed", e);
        }
    }

    public DownloadResult download(Long attachmentId) {
        String tenantId = TenantContext.getTenant();

        Attachment attachment = attachmentRepository.findByIdAndTenantId(attachmentId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Attachment not found"));

        try {
            InputStream is = minioService.downloadFile(attachment.getObjectKey());

            return new DownloadResult(
                    new InputStreamResource(is),
                    attachment.getOriginalName(),
                    attachment.getMimeType(),
                    attachment.getSize()
            );
        } catch (Exception e) {
            throw new NoSuchElementException("Attachment download failed", e);
        }
    }
}