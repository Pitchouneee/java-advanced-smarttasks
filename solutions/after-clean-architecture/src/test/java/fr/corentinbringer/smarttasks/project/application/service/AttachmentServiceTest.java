package fr.corentinbringer.smarttasks.project.application.service;

import fr.corentinbringer.smarttasks.exception.InvalidRequestException;
import fr.corentinbringer.smarttasks.configuration.tenant.TenantContext;
import fr.corentinbringer.smarttasks.exception.StorageException;
import fr.corentinbringer.smarttasks.project.application.model.FileContent;
import fr.corentinbringer.smarttasks.project.application.port.out.AttachmentPort;
import fr.corentinbringer.smarttasks.project.application.port.out.FileStoragePort;
import fr.corentinbringer.smarttasks.project.domain.model.Attachment;
import fr.corentinbringer.smarttasks.project.domain.model.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Teste la compensation de l'upload (chap. 05, §5.2) sans S3 ni PostgreSQL :
 * avec des mocks, on peut provoquer à volonté une panne de base de données.
 */
@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private AttachmentPort attachmentPort;
    @Mock
    private TaskService taskService;
    @Mock
    private FileStoragePort fileStoragePort;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AttachmentService attachmentService;

    private final Task task = new Task(10L, "alice", 42L, "Tâche", null, null, false, Instant.now());

    @BeforeEach
    void setUp() {
        TenantContext.setTenant("alice");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private static FileContent pdf() {
        byte[] bytes = "%PDF-1.7".getBytes();
        return new FileContent("rapport final.pdf", "application/pdf", bytes.length, new ByteArrayInputStream(bytes));
    }

    @Test
    void create_storesTheFileThenSavesItsMetadata() {
        FileContent file = pdf();
        when(taskService.findById(10L)).thenReturn(task);
        when(fileStoragePort.uploadFile(file)).thenReturn("key-123");
        when(attachmentPort.save(any())).then(returnsFirstArg());

        Attachment result = attachmentService.create(10L, file);

        assertThat(result.objectKey()).isEqualTo("key-123");
        assertThat(result.originalName()).isEqualTo("rapport final.pdf");
        assertThat(result.tenantId()).isEqualTo("alice");
        assertThat(result.taskId()).isEqualTo(10L);
        verify(fileStoragePort, never()).deleteFile(any());
    }

    @Test
    void create_deletesTheStoredFileWhenTheDatabaseFails() {
        FileContent file = pdf();
        RuntimeException dbFailure = new RuntimeException("Database unavailable");
        when(taskService.findById(10L)).thenReturn(task);
        when(fileStoragePort.uploadFile(file)).thenReturn("key-123");
        when(attachmentPort.save(any())).thenThrow(dbFailure);

        assertThatThrownBy(() -> attachmentService.create(10L, file)).isSameAs(dbFailure);

        // Compensation : le fichier envoyé dans S3 ne doit pas rester orphelin
        verify(fileStoragePort).deleteFile("key-123");
    }

    @Test
    void create_rethrowsTheOriginalErrorEvenIfTheCompensationFails() {
        FileContent file = pdf();
        RuntimeException dbFailure = new RuntimeException("Database unavailable");
        when(taskService.findById(10L)).thenReturn(task);
        when(fileStoragePort.uploadFile(file)).thenReturn("key-123");
        when(attachmentPort.save(any())).thenThrow(dbFailure);
        doThrow(new StorageException("S3 down", null)).when(fileStoragePort).deleteFile("key-123");

        // C'est la panne de base qui explique l'échec, pas celle de la compensation
        assertThatThrownBy(() -> attachmentService.create(10L, file)).isSameAs(dbFailure);
    }

    @Test
    void create_rejectsAnEmptyFileBeforeTouchingTheStorage() {
        FileContent empty = new FileContent("vide.txt", "text/plain", 0, new ByteArrayInputStream(new byte[0]));

        assertThatThrownBy(() -> attachmentService.create(10L, empty))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(fileStoragePort, attachmentPort);
    }
}
