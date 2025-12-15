package fr.corentinbringer.smarttasks.project.infrastructure.web.model;

public record AttachmentResponse(
        Long id,
        String originalName,
        String mimeType,
        long size,
        String data
) {}
