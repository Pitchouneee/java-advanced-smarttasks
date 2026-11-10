package fr.corentinbringer.smarttasks.project.infrastructure.web;

import fr.corentinbringer.smarttasks.project.application.model.FileContent;
import fr.corentinbringer.smarttasks.project.application.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Attachments", description = "Attachment operations")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @Operation(
            summary = "Download an attachment",
            description = "Download the specified attachment file."
    )
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(@Parameter(description = "Attachment ID to download") @PathVariable Long id) {
        FileContent file = attachmentService.download(id);

        // Génère : attachment; filename="=?UTF-8?Q?...?="; filename*=UTF-8''rapport%20d%C3%A9cembre.pdf
        // filename* (RFC 6266 / 5987) conserve espaces et accents ; Spring s'occupe de l'encodage.
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(file.name(), StandardCharsets.UTF_8)
                .build();

        // Le type MIME vient du client à l'upload : il peut être absent
        MediaType contentType = file.contentType() != null
                ? MediaType.parseMediaType(file.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(contentType)
                .contentLength(file.size())
                // InputStreamResource : Spring copie le flux S3 vers la réponse HTTP, puis le ferme
                .body(new InputStreamResource(file.content()));
    }

    @Operation(
            summary = "Delete an attachment",
            description = "Delete an attachment (database row and stored file)."
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Attachment ID to delete") @PathVariable Long id) {
        attachmentService.delete(id);
    }
}
