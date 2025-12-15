package fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.repository;

import fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.entity.TaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    @Query("SELECT t FROM TaskEntity t WHERE t.project.id = :projectId AND t.tenantId = :tenantId ORDER BY t.createdOn DESC")
    Page<TaskEntity> findAllByProjectIdAndTenantId(@Param("projectId") Long projectId, @Param("tenantId") String tenantId, Pageable pageable);

    Optional<TaskEntity> findByIdAndTenantId(Long id, String tenantId);

    long countByTenantId(String tenantId);

    @Query("""
           SELECT count(t)
           FROM TaskEntity t
           WHERE t.tenantId = :tenantId
           AND t.completed = false
           AND t.dueDate IS NOT NULL
           AND t.dueDate < :today
           """)
    long countOverdueTasksByTenantId(@Param("tenantId") String tenantId, @Param("today") LocalDate today);
}
