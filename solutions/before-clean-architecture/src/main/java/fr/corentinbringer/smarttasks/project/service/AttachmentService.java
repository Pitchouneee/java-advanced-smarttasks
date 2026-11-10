package fr.corentinbringer.smarttasks.project.service;

import fr.corentinbringer.smarttasks.exception.ResourceNotFoundException;
import fr.corentinbringer.smarttasks.exception.InvalidRequestException;
import fr.corentinbringer.smarttasks.configuration.tenant.TenantContext;
import fr.corentinbringer.smarttasks.storage.service.S3StorageService;
import fr.corentinbringer.smarttasks.project.event.AttachmentsDeletedEvent;
import fr.corentinbringer.smarttasks.project.model.Attachment;
import fr.corentinbringer.smarttasks.project.model.AttachmentResponse;
import fr.corentinbringer.smarttasks.project.model.DownloadResult;
import fr.corentinbringer.smarttasks.project.model.Task;
import fr.corentinbringer.smarttasks.project.repository.AttachmentRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskService taskService;
    private final S3StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Page<AttachmentResponse> findAllByTaskId(Long taskId, Pageable pageable) {
        Task task = taskService.findById(taskId);

        return attachmentRepository.findAllByTaskIdAndTenantId(task.getId(), TenantContext.getTenant(), pageable);
    }

    /**
     * Upload d'une pièce jointe : deux systèmes à mettre à jour (S3 puis PostgreSQL),
     * sans transaction commune possible entre les deux.
     *
     * Pas de @Transactional ici :
     * - une transaction ne peut pas annuler un fichier déjà envoyé dans S3 ;
     * - save() s'exécute dans sa propre transaction : s'il rend la main, la ligne est commitée ;
     *   s'il lève une exception, rien n'est écrit en base.
     * On peut donc compenser à coup sûr : si la base échoue, on supprime le fichier envoyé.
     * (C'est le principe du pattern SAGA, revu au chapitre 08.)
     */
    public AttachmentResponse create(Long taskId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidRequestException("File is empty"); // 400 : erreur du client
        }

        String tenantId = TenantContext.getTenant();
        Task task = taskService.findById(taskId); // 404 si la tâche n'est pas au tenant courant

        // 1. Stockage du fichier. En cas d'échec : StorageException (500), rien à annuler.
        String objectKey = storageService.uploadFile(file);

        // 2. Écriture des métadonnées. En cas d'échec : on compense en supprimant le fichier.
        try {
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
                    "/api/attachments/" + savedAttachment.getId() + "/download",
                    savedAttachment.getCreatedOn()
            );

        } catch (RuntimeException e) {
            deleteQuietly(objectKey);
            throw e; // on relance l'erreur d'origine : c'est elle qui explique l'échec
        }
    }

    /** Action de compensation : ne doit jamais masquer l'erreur d'origine. */
    private void deleteQuietly(String objectKey) {
        try {
            storageService.deleteFile(objectKey);
        } catch (RuntimeException e) {
            log.warn("Compensation failed: object {} is orphaned in the bucket", objectKey, e);
        }
    }

    @Transactional
    public void delete(Long attachmentId) {
        Attachment attachment = attachmentRepository.findByIdAndTenantId(attachmentId, TenantContext.getTenant())
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        attachmentRepository.delete(attachment);

        // Le fichier sera effacé du stockage après le commit (AttachmentStorageCleanupListener).
        eventPublisher.publishEvent(new AttachmentsDeletedEvent(List.of(attachment.getObjectKey())));
    }

    public DownloadResult download(Long attachmentId) {
        String tenantId = TenantContext.getTenant();

        Attachment attachment = attachmentRepository.findByIdAndTenantId(attachmentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        // StorageException (500) si le fichier est introuvable dans le bucket
        InputStream is = storageService.downloadFile(attachment.getObjectKey());

        return new DownloadResult(
                new InputStreamResource(is),
                attachment.getOriginalName(),
                attachment.getMimeType(),
                attachment.getSize()
        );
    }
}