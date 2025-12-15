package fr.corentinbringer.smarttasks.project.infrastructure.persistence.adapter;

import fr.corentinbringer.smarttasks.project.application.port.out.TaskPort;
import fr.corentinbringer.smarttasks.project.domain.model.Task;
import fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.entity.TaskEntity;
import fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.mapper.TaskMapper;
import fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TaskPersistenceAdapter implements TaskPort {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    @Override
    public Page<Task> findAllByProjectIdAndTenantId(Long projectId, String tenantId, Pageable pageable) {
        Page<TaskEntity> entityPage = taskRepository.findAllByProjectIdAndTenantId(projectId, tenantId, pageable);
        return entityPage.map(taskMapper::toDomain);
    }

    @Override
    public Optional<Task> findByIdAndTenantId(Long id, String tenantId) {
        return taskRepository.findByIdAndTenantId(id, tenantId).map(taskMapper::toDomain);
    }

    @Override
    public Task save(Task task) {
        TaskEntity entity = taskMapper.toEntity(task);
        TaskEntity savedEntity = taskRepository.save(entity);
        return taskMapper.toDomain(savedEntity);
    }

    @Override
    public long countByTenantId(String tenantId) {
        return taskRepository.countByTenantId(tenantId);
    }

    @Override
    public long countOverdueTasksByTenantId(String tenantId, LocalDate today) {
        return taskRepository.countOverdueTasksByTenantId(tenantId, today);
    }
}
