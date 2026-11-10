package fr.corentinbringer.smarttasks.project.controller;

import fr.corentinbringer.smarttasks.project.model.DownloadResult;
import fr.corentinbringer.smarttasks.project.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(@PathVariable Long id) {
        DownloadResult result = attachmentService.download(id);

        // Génère : attachment; filename="=?UTF-8?Q?...?="; filename*=UTF-8''rapport%20d%C3%A9cembre.pdf
        // filename* (RFC 6266 / 5987) conserve espaces et accents ; Spring s'occupe de l'encodage.
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(result.fileName(), StandardCharsets.UTF_8)
                .build();

        // Le type MIME vient du client à l'upload : il peut être absent
        MediaType contentType = result.mimeType() != null
                ? MediaType.parseMediaType(result.mimeType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(contentType)
                .contentLength(result.size())
                .body(result.resource());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        attachmentService.delete(id);
    }
}
