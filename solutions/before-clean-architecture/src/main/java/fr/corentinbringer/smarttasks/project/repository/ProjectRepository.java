package fr.corentinbringer.smarttasks.project.repository;

import fr.corentinbringer.smarttasks.project.model.Project;
import fr.corentinbringer.smarttasks.project.model.ProjectListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("""
           SELECT new fr.corentinbringer.smarttasks.project.model.ProjectListResponse(
               p.id,
               p.name,
               p.createdOn
           )
           FROM Project p
           WHERE p.tenantId = :tenantId
           """)
    Page<ProjectListResponse> findAllListByTenantId(@Param("tenantId") String tenantId, Pageable pageable);

    Optional<Project> findByIdAndTenantId(Long id, String tenantId);

    /** Projets "actifs" : au moins une tâche non terminée. */
    @Query("""
           SELECT count(p)
           FROM Project p
           WHERE p.tenantId = :tenantId
           AND EXISTS (SELECT t FROM Task t WHERE t.project = p AND t.completed = false)
           """)
    long countActiveByTenantId(@Param("tenantId") String tenantId);

    @Query("""
           SELECT new fr.corentinbringer.smarttasks.project.model.ProjectListResponse(
               p.id,
               p.name,
               p.createdOn
           )
           FROM Project p
           WHERE p.tenantId = :tenantId
           ORDER BY p.createdOn DESC
           """)
    List<ProjectListResponse> findLatestProjectsByTenantId(@Param("tenantId") String tenantId, Pageable pageable);
}
