package fr.corentinbringer.smarttasks.project.model;

import java.time.Instant;

public record ProjectListResponse(
        Long id,
        String name,
        Instant createdOn
) {}