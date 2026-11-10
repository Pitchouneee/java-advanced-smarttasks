package fr.corentinbringer.smarttasks.project.infrastructure.web.model;

import fr.corentinbringer.smarttasks.project.domain.model.Attachment;
import java.time.Instant;

public record AttachmentResponse(
        Long id,
        String originalName,
        String mimeType,
        long size,
        String data,
        Instant createdOn
) {

    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(
                attachment.id(),
                attachment.originalName(),
                attachment.mimeType(),
                attachment.size(),
                // L'URL de téléchargement est un détail de l'API REST : elle est construite ici
                "/api/attachments/" + attachment.id() + "/download",
                attachment.createdOn()
        );
    }
}
