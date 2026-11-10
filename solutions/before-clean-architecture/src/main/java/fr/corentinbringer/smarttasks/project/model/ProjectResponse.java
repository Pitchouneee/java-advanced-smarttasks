package fr.corentinbringer.smarttasks.project.model;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        Instant createdOn
) {}
