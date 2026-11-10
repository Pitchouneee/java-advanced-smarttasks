package fr.corentinbringer.smarttasks.project.application.service;

import fr.corentinbringer.smarttasks.exception.ResourceNotFoundException;
import fr.corentinbringer.smarttasks.configuration.tenant.TenantContext;
import fr.corentinbringer.smarttasks.project.application.port.out.AttachmentPort;
import fr.corentinbringer.smarttasks.project.application.port.out.ProjectPort;
import fr.corentinbringer.smarttasks.project.domain.event.AttachmentsDeletedEvent;
import fr.corentinbringer.smarttasks.project.domain.model.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Cas d'usage autour des projets.
 * Entrées : des types simples ; sorties : des objets du domaine.
 * La conversion depuis / vers le JSON de l'API est faite par les controllers (couche web).
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectPort projectPort;
    private final AttachmentPort attachmentPort;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<Project> findAll(Pageable pageable) {
        return projectPort.findAll(TenantContext.getTenant(), pageable);
    }

    @Transactional
    public Project create(String name) {
        return projectPort.save(Project.create(TenantContext.getTenant(), name, Instant.now()));
    }

    @Transactional(readOnly = true)
    public Project findById(Long projectId) {
        return projectPort.findByIdAndTenantId(projectId, TenantContext.getTenant())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    @Transactional
    public Project rename(Long projectId, String newName) {
        // Le domaine est immuable (record) : rename() renvoie une nouvelle version du projet.
        return projectPort.save(findById(projectId).rename(newName));
    }

    @Transactional
    public void delete(Long projectId) {
        String tenantId = TenantContext.getTenant();
        Project project = findById(projectId);

        // À récupérer AVANT la suppression : après, les lignes n'existent plus.
        List<String> objectKeys = attachmentPort.findObjectKeysByProjectIdAndTenantId(project.id(), tenantId);

        projectPort.deleteById(project.id());

        // Les fichiers seront effacés du stockage après le commit (AttachmentStorageCleanupListener).
        eventPublisher.publishEvent(new AttachmentsDeletedEvent(objectKeys));
    }

    /** Projets "actifs" : ceux qui ont au moins une tâche non terminée. */
    public long countActiveProjects() {
        return projectPort.countActiveByTenantId(TenantContext.getTenant());
    }

    public List<Project> findLatestProjects(int limit) {
        return projectPort.findLatestProjectsByTenantId(TenantContext.getTenant(), limit);
    }
}
