package fr.corentinbringer.smarttasks.project.domain.model;

import java.time.Instant;

/**
 * Un projet du domaine. Immuable : rename() renvoie une nouvelle instance.
 */
public record Project(
        Long id,
        String tenantId,
        String name,
        Instant createdOn
) {

    public Project {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A project must have a name");
        }
    }

    public static Project create(String tenantId, String name, Instant now) {
        return new Project(null, tenantId, name, now);
    }

    public Project rename(String newName) {
        return new Project(id, tenantId, newName, createdOn);
    }
}
