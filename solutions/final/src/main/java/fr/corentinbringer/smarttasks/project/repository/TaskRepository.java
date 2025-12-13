package fr.corentinbringer.smarttasks.project.repository;

import fr.corentinbringer.smarttasks.project.model.Task;
import fr.corentinbringer.smarttasks.project.model.TaskListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("""
           SELECT new fr.corentinbringer.smarttasks.project.model.TaskListResponse(
               t.id,
               t.title,
               t.description,
               t.dueDate,
               t.completed
           )
           FROM Task t
           WHERE t.project.id = :projectId AND t.tenantId = :tenantId
           ORDER BY t.createdOn DESC
           """)
    Page<TaskListResponse> findAllByProjectIdAndTenantId(@Param("projectId") Long projectId, @Param("tenantId") String tenantId, Pageable pageable);

    Optional<Task> findByIdAndTenantId(Long id, String tenantId);
}
