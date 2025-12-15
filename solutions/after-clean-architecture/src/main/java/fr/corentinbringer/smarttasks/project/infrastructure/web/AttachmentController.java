package fr.corentinbringer.smarttasks.project.infrastructure.web;

import fr.corentinbringer.smarttasks.project.infrastructure.web.model.DownloadResult;
import fr.corentinbringer.smarttasks.project.application.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "Attachment operations")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @Operation(
            summary = "Download an attachment",
            description = "Download the specified attachment file."
    )
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(@Parameter(description = "Attachment ID to download") @PathVariable Long id) {
        DownloadResult result = attachmentService.download(id);

        String encodedFileName = URLEncoder.encode(result.fileName(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                .header(HttpHeaders.CONTENT_TYPE, result.mimeType())
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(result.size()))
                .body(result.resource());
    }
}
