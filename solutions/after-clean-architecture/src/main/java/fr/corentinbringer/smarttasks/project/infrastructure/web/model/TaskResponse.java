package fr.corentinbringer.smarttasks.project.infrastructure.web.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        LocalDate dueDate,
        boolean completed,
        Long projectId,
        LocalDateTime createdOn
) {}
