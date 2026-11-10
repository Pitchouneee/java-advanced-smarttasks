package fr.corentinbringer.smarttasks.project.infrastructure.web.model;

import fr.corentinbringer.smarttasks.project.domain.model.Project;
import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        Instant createdOn
) {

    /** Conversion domaine → JSON : c'est le rôle de la couche web, pas du service. */
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.id(), project.name(), project.createdOn());
    }
}
