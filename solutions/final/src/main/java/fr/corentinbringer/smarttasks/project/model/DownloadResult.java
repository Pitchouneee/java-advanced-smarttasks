package fr.corentinbringer.smarttasks.project.model;

import org.springframework.core.io.InputStreamResource;

public record DownloadResult(
        InputStreamResource resource,
        String fileName,
        String mimeType,
        long size
) {}
