package fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.repository;

import fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.entity.AttachmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttachmentRepository extends JpaRepository<AttachmentEntity, Long> {

    @Query("SELECT a FROM AttachmentEntity a WHERE a.task.id = :taskId AND a.tenantId = :tenantId ORDER BY a.uploadedOn DESC")
    Page<AttachmentEntity> findAllByTaskIdAndTenantId(@Param("taskId") Long taskId, @Param("tenantId") String tenantId, Pageable pageable);

    Optional<AttachmentEntity> findByIdAndTenantId(Long id, String tenantId);
}
