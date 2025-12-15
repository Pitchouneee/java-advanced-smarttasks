package fr.corentinbringer.smarttasks.project.model;

import jakarta.validation.constraints.Size;

public record ProjectCreateRequest(
        @Size(min = 3, max = 50)
        String name
) {}