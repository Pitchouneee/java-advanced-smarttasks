package fr.corentinbringer.smarttasks.project.application.service;

import fr.corentinbringer.smarttasks.exception.ResourceNotFoundException;
import fr.corentinbringer.smarttasks.configuration.tenant.TenantContext;
import fr.corentinbringer.smarttasks.project.application.model.CreateTaskCommand;
import fr.corentinbringer.smarttasks.project.application.model.UpdateTaskCommand;
import fr.corentinbringer.smarttasks.project.application.port.out.AttachmentPort;
import fr.corentinbringer.smarttasks.project.application.port.out.TaskPort;
import fr.corentinbringer.smarttasks.project.domain.event.AttachmentsDeletedEvent;
import fr.corentinbringer.smarttasks.project.domain.model.Project;
import fr.corentinbringer.smarttasks.project.domain.model.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test UNITAIRE : pas de Spring, pas de base de données.
 * Les ports sont remplacés par des mocks Mockito : c'est possible parce que
 * TaskService dépend d'interfaces (TaskPort, AttachmentPort) et non de JPA (chap. 06).
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskPort taskPort;
    @Mock
    private ProjectService projectService;
    @Mock
    private AttachmentPort attachmentPort;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        // En production, c'est le TenantFilter qui remplit le contexte à partir du JWT
        TenantContext.setTenant("alice");
    }

    @AfterEach
    void tearDown() {
        // ThreadLocal : toujours nettoyer, sinon le tenant "fuit" vers le test suivant
        TenantContext.clear();
    }

    @Test
    void create_attachesTheTaskToTheProjectWithTheCurrentTenant() {
        Project project = new Project(42L, "alice", "Demo", Instant.now());
        when(projectService.findById(42L)).thenReturn(project);
        when(taskPort.save(any())).then(returnsFirstArg());

        taskService.create(42L, new CreateTaskCommand("Écrire les tests", "JUnit + Mockito", LocalDate.of(2026, 12, 1)));

        // ArgumentCaptor : on récupère ce que le service a réellement envoyé au port
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskPort).save(captor.capture());
        Task saved = captor.getValue();

        assertThat(saved.id()).isNull();
        assertThat(saved.tenantId()).isEqualTo("alice");
        assertThat(saved.projectId()).isEqualTo(42L);
        assertThat(saved.title()).isEqualTo("Écrire les tests");
        assertThat(saved.completed()).isFalse();
        assertThat(saved.createdOn()).isNotNull();
    }

    @Test
    void create_failsWithoutSavingWhenTheProjectIsNotOwnedByTheCurrentTenant() {
        // ProjectService.findById filtre par tenant : un projet de Bob est "introuvable" pour Alice
        when(projectService.findById(99L)).thenThrow(new ResourceNotFoundException("Project not found"));

        assertThatThrownBy(() -> taskService.create(99L, new CreateTaskCommand("Intrusion", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(taskPort);
    }

    @Test
    void update_changesEditableFieldsAndKeepsTheOthers() {
        Instant createdOn = Instant.parse("2026-01-10T08:00:00Z");
        Task existing = new Task(7L, "alice", 42L, "Ancien titre", null, null, false, createdOn);
        when(taskPort.findByIdAndTenantId(7L, "alice")).thenReturn(Optional.of(existing));
        when(taskPort.save(any())).then(returnsFirstArg());

        Task updated = taskService.update(7L, new UpdateTaskCommand("Nouveau titre", "Détails", null, true));

        assertThat(updated.title()).isEqualTo("Nouveau titre");
        assertThat(updated.completed()).isTrue();
        // Champs non modifiables : identiques à l'existant
        assertThat(updated.id()).isEqualTo(7L);
        assertThat(updated.tenantId()).isEqualTo("alice");
        assertThat(updated.projectId()).isEqualTo(42L);
        assertThat(updated.createdOn()).isEqualTo(createdOn);
    }

    @Test
    void delete_publishesTheObjectKeysSoThatFilesAreRemovedAfterCommit() {
        Task task = new Task(7L, "alice", 42L, "Tâche", null, null, false, Instant.now());
        when(taskPort.findByIdAndTenantId(7L, "alice")).thenReturn(Optional.of(task));
        when(attachmentPort.findObjectKeysByTaskIdAndTenantId(7L, "alice")).thenReturn(List.of("key-1", "key-2"));

        taskService.delete(7L);

        verify(taskPort).deleteById(7L);
        verify(eventPublisher).publishEvent(new AttachmentsDeletedEvent(List.of("key-1", "key-2")));
    }
}
