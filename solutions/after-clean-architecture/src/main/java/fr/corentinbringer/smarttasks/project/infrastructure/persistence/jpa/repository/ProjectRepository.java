package fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.repository;

import fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

    @Query("SELECT p FROM ProjectEntity p WHERE p.tenantId = :tenantId")
    Page<ProjectEntity> findAllByTenantId(@Param("tenantId") String tenantId, Pageable pageable);

    Optional<ProjectEntity> findByIdAndTenantId(Long id, String tenantId);

    long countByTenantId(String tenantId);

    @Query("""
           SELECT p
           FROM ProjectEntity p
           WHERE p.tenantId = :tenantId
           ORDER BY p.createdOn DESC
           """)
    List<ProjectEntity> findLatestProjectsByTenantId(@Param("tenantId") String tenantId, Pageable pageable);
}
