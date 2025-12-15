package fr.corentinbringer.smarttasks.project.infrastructure.web.model;

import java.time.LocalDate;

public record TaskListResponse(
        Long id,
        String title,
        String description,
        LocalDate dueDate,
        boolean completed
) {}
