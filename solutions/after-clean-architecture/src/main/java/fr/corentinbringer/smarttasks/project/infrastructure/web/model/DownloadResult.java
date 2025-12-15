package fr.corentinbringer.smarttasks.project.infrastructure.web.model;

import org.springframework.core.io.InputStreamResource;

public record DownloadResult(
        InputStreamResource resource,
        String fileName,
        String mimeType,
        long size
) {}
