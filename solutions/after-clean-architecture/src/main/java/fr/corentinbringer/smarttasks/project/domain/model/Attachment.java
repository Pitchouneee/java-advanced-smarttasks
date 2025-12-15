package fr.corentinbringer.smarttasks.project.domain.model;

import java.time.LocalDateTime;

public record Attachment(
        Long id,
        String tenantId,
        String objectKey,
        String originalName,
        String mimeType,
        long size,
        LocalDateTime uploadedOn,
        Long taskId
) {}