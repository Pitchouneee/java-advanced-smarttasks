package fr.corentinbringer.smarttasks.project.infrastructure.web.model;

import fr.corentinbringer.smarttasks.project.domain.model.Project;
import java.time.Instant;

public record ProjectListResponse(
        Long id,
        String name,
        Instant createdOn
) {

    public static ProjectListResponse from(Project project) {
        return new ProjectListResponse(project.id(), project.name(), project.createdOn());
    }
}
