package fr.corentinbringer.smarttasks.project.application.service;

import fr.corentinbringer.smarttasks.exception.ResourceNotFoundException;
import fr.corentinbringer.smarttasks.exception.InvalidRequestException;
import fr.corentinbringer.smarttasks.configuration.tenant.TenantContext;
import fr.corentinbringer.smarttasks.project.application.model.FileContent;
import fr.corentinbringer.smarttasks.project.application.port.out.AttachmentPort;
import fr.corentinbringer.smarttasks.project.application.port.out.FileStoragePort;
import fr.corentinbringer.smarttasks.project.domain.event.AttachmentsDeletedEvent;
import fr.corentinbringer.smarttasks.project.domain.model.Attachment;
import fr.corentinbringer.smarttasks.project.domain.model.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Cas d'usage autour des pièces jointes.
 * Aucun type de Spring Web ici (MultipartFile, InputStreamResource...) : les fichiers
 * circulent sous forme de FileContent, et c'est le controller qui fait la traduction HTTP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentPort attachmentPort;
    private final TaskService taskService;
    private final FileStoragePort fileStoragePort;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<Attachment> findAllByTaskId(Long taskId, Pageable pageable) {
        Task task = taskService.findById(taskId);

        return attachmentPort.findAllByTaskIdAndTenantId(task.id(), TenantContext.getTenant(), pageable);
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
    public Attachment create(Long taskId, FileContent file) {
        if (file.size() == 0) {
            throw new InvalidRequestException("File is empty"); // 400 : erreur du client
        }

        String tenantId = TenantContext.getTenant();
        Task task = taskService.findById(taskId); // 404 si la tâche n'est pas au tenant courant

        // 1. Stockage du fichier. En cas d'échec : StorageException (500), rien à annuler.
        String objectKey = fileStoragePort.uploadFile(file);

        // 2. Écriture des métadonnées. En cas d'échec : on compense en supprimant le fichier.
        try {
            Attachment attachment = new Attachment(
                    null,
                    tenantId,
                    objectKey,
                    file.name(),
                    file.contentType(),
                    file.size(),
                    Instant.now(),
                    task.id()
            );

            return attachmentPort.save(attachment);

        } catch (RuntimeException e) {
            deleteQuietly(objectKey);
            throw e; // on relance l'erreur d'origine : c'est elle qui explique l'échec
        }
    }

    /** Action de compensation : ne doit jamais masquer l'erreur d'origine. */
    private void deleteQuietly(String objectKey) {
        try {
            fileStoragePort.deleteFile(objectKey);
        } catch (RuntimeException e) {
            log.warn("Compensation failed: object {} is orphaned in the bucket", objectKey, e);
        }
    }

    @Transactional
    public void delete(Long attachmentId) {
        Attachment attachment = attachmentPort.findByIdAndTenantId(attachmentId, TenantContext.getTenant())
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        attachmentPort.deleteById(attachment.id());

        // Le fichier sera effacé du stockage après le commit (AttachmentStorageCleanupListener).
        eventPublisher.publishEvent(new AttachmentsDeletedEvent(List.of(attachment.objectKey())));
    }

    public FileContent download(Long attachmentId) {
        Attachment attachment = attachmentPort.findByIdAndTenantId(attachmentId, TenantContext.getTenant())
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        // StorageException (500) si le fichier est introuvable dans le bucket
        return new FileContent(
                attachment.originalName(),
                attachment.mimeType(),
                attachment.size(),
                fileStoragePort.downloadFile(attachment.objectKey())
        );
    }
}
