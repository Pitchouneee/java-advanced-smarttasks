package fr.corentinbringer.smarttasks.project.infrastructure.web.model;

import java.time.LocalDateTime;

public record ProjectListResponse(
        Long id,
        String name,
        LocalDateTime createdOn
) {}