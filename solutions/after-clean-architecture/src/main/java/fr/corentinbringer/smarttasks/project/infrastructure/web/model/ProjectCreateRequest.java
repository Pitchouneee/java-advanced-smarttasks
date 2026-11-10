package fr.corentinbringer.smarttasks.project.infrastructure.web.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectCreateRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        String name
) {}