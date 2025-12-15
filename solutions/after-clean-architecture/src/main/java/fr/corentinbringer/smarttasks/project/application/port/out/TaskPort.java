package fr.corentinbringer.smarttasks.project.application.port.out;

import fr.corentinbringer.smarttasks.project.domain.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface TaskPort {

    Page<Task> findAllByProjectIdAndTenantId(Long projectId, String tenantId, Pageable pageable);

    Optional<Task> findByIdAndTenantId(Long id, String tenantId);

    Task save(Task task);

    long countByTenantId(String tenantId);

    long countOverdueTasksByTenantId(String tenantId, LocalDate today);
}
