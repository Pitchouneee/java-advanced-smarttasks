package fr.corentinbringer.smarttasks.project.infrastructure.web.model;

import fr.corentinbringer.smarttasks.project.domain.model.Task;
import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String title,
        String description,
        LocalDate dueDate,
        boolean completed,
        Long projectId,
        Instant createdOn
) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.id(),
                task.title(),
                task.description(),
                task.dueDate(),
                task.completed(),
                task.projectId(),
                task.createdOn()
        );
    }
}
