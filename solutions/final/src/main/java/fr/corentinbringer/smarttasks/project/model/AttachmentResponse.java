package fr.corentinbringer.smarttasks.project.model;

public record AttachmentResponse(
        Long id,
        String originalName,
        String mimeType,
        long size,
        String data
) {}
