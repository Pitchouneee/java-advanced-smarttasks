package fr.corentinbringer.smarttasks.project.application.model;

import java.time.LocalDate;

/** Nouvelles valeurs des champs modifiables d'une tâche (voir CreateTaskCommand). */
public record UpdateTaskCommand(
        String title,
        String description,
        LocalDate dueDate,
        boolean completed
) {}
