package fr.corentinbringer.smarttasks.project.application.model;

import java.time.LocalDate;

/**
 * Données nécessaires pour créer une tâche, côté application.
 *
 * Pourquoi pas TaskCreateRequest ? Ce DTO appartient à la couche web (JSON, @Valid...).
 * Le service ne doit pas dépendre du format de l'API : le controller convertit la requête en commande.
 */
public record CreateTaskCommand(
        String title,
        String description,
        LocalDate dueDate
) {}
