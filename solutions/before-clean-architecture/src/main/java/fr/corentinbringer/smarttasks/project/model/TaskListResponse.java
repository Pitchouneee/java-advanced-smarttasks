package fr.corentinbringer.smarttasks.project.model;

import java.time.LocalDate;

public record TaskListResponse(
        Long id,
        String title,
        String description,
        LocalDate dueDate,
        boolean completed
) {}
