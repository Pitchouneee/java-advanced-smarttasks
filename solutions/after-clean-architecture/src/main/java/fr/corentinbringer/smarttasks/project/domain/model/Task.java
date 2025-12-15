package fr.corentinbringer.smarttasks.project.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Task(
        Long id,
        String tenantId,
        Long projectId,
        String title,
        String description,
        LocalDate dueDate,
        boolean completed,
        LocalDateTime createdOn
) {}