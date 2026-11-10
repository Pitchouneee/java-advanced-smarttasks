package fr.corentinbringer.smarttasks.project.domain.model;

import java.time.Instant;

public record Attachment(
        Long id,
        String tenantId,
        String objectKey,
        String originalName,
        String mimeType,
        long size,
        Instant createdOn,
        Long taskId
) {}