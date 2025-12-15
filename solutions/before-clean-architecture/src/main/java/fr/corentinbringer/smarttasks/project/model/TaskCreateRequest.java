package fr.corentinbringer.smarttasks.project.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskCreateRequest(
        @NotBlank @Size(max = 100) String title,
        String description,
        LocalDate dueDate
) {}
