package fr.corentinbringer.smarttasks.project.repository;

import fr.corentinbringer.smarttasks.project.model.Attachment;
import fr.corentinbringer.smarttasks.project.model.AttachmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    @Query("""
            SELECT new fr.corentinbringer.smarttasks.project.model.AttachmentResponse(
                a.id,
                a.originalName,
                a.mimeType,
                a.size,
                '/api/attachments/' || a.id || '/download',
                a.createdOn
            )
            FROM Attachment a
            WHERE a.task.id = :taskId AND a.tenantId = :tenantId
            ORDER BY a.createdOn DESC
            """)
    Page<AttachmentResponse> findAllByTaskIdAndTenantId(@Param("taskId") Long taskId, @Param("tenantId") String tenantId, Pageable pageable);

    Optional<Attachment> findByIdAndTenantId(Long id, String tenantId);

    @Query("SELECT a.objectKey FROM Attachment a WHERE a.task.id = :taskId AND a.tenantId = :tenantId")
    List<String> findObjectKeysByTaskIdAndTenantId(@Param("taskId") Long taskId, @Param("tenantId") String tenantId);

    @Query("SELECT a.objectKey FROM Attachment a WHERE a.task.project.id = :projectId AND a.tenantId = :tenantId")
    List<String> findObjectKeysByProjectIdAndTenantId(@Param("projectId") Long projectId, @Param("tenantId") String tenantId);
}
