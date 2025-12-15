package fr.corentinbringer.smarttasks.project.infrastructure.web.model;

import jakarta.validation.constraints.Size;

public record ProjectCreateRequest(
        @Size(min = 3, max = 50)
        String name
) {}