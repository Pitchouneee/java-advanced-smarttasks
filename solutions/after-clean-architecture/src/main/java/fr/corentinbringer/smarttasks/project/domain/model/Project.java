package fr.corentinbringer.smarttasks.project.domain.model;

import java.time.LocalDateTime;

public record Project(
        Long id,
        String tenantId,
        String name,
        LocalDateTime createdOn
) {}
