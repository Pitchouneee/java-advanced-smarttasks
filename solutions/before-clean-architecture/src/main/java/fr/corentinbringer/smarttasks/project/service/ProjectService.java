package fr.corentinbringer.smarttasks.project.service;

import fr.corentinbringer.smarttasks.exception.ResourceNotFoundException;
import fr.corentinbringer.smarttasks.configuration.tenant.TenantContext;
import fr.corentinbringer.smarttasks.project.event.AttachmentsDeletedEvent;
import fr.corentinbringer.smarttasks.project.model.Project;
import fr.corentinbringer.smarttasks.project.model.ProjectCreateRequest;
import fr.corentinbringer.smarttasks.project.model.ProjectListResponse;
import fr.corentinbringer.smarttasks.project.model.ProjectResponse;
import fr.corentinbringer.smarttasks.project.model.ProjectUpdateRequest;
import fr.corentinbringer.smarttasks.project.repository.AttachmentRepository;
import fr.corentinbringer.smarttasks.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final AttachmentRepository attachmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Page<ProjectListResponse> findAll(Pageable pageable) {
        String tenantId = TenantContext.getTenant();
        return projectRepository.findAllListByTenantId(tenantId, pageable);
    }

    public ProjectResponse create(ProjectCreateRequest request) {
        Project project = new Project();
        project.setName(request.name());
        project.setTenantId(TenantContext.getTenant());
        Project savedProject = projectRepository.save(project);

        return new ProjectResponse(
                savedProject.getId(),
                savedProject.getName(),
                savedProject.getCreatedOn()
        );
    }

    public Project findById(Long projectId) {
        return projectRepository.findByIdAndTenantId(projectId, TenantContext.getTenant())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    public ProjectResponse findByIdResponse(Long projectId) {
        Project project = findById(projectId);

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getCreatedOn()
        );
    }

    @Transactional
    public ProjectResponse update(Long projectId, ProjectUpdateRequest request) {
        Project project = findById(projectId);

        // L'entité est "managed" (chargée dans la transaction) : Hibernate détecte la
        // modification (dirty checking) et fait l'UPDATE au commit, sans appel à save().
        project.setName(request.name());

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getCreatedOn()
        );
    }

    @Transactional
    public void delete(Long projectId) {
        String tenantId = TenantContext.getTenant();
        Project project = findById(projectId);

        // À récupérer AVANT la suppression : après, les lignes n'existent plus.
        List<String> objectKeys = attachmentRepository.findObjectKeysByProjectIdAndTenantId(project.getId(), tenantId);

        // cascade = ALL : supprime aussi les tâches, puis leurs pièces jointes, en base.
        projectRepository.delete(project);

        // Les fichiers seront effacés du stockage après le commit (AttachmentStorageCleanupListener).
        eventPublisher.publishEvent(new AttachmentsDeletedEvent(objectKeys));
    }

    /** Projets "actifs" : ceux qui ont au moins une tâche non terminée. */
    public long countActiveProjects() {
        return projectRepository.countActiveByTenantId(TenantContext.getTenant());
    }

    public List<ProjectListResponse> findLatestProjects(int limit) {
        String tenantId = TenantContext.getTenant();
        Pageable pageable = PageRequest.of(0, limit);
        return projectRepository.findLatestProjectsByTenantId(tenantId, pageable);
    }
}
