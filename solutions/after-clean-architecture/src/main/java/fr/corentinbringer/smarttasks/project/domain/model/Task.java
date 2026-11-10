package fr.corentinbringer.smarttasks.project.domain.model;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Une tâche du domaine : des données ET des règles métier.
 *
 * Le record est immuable : chaque "modification" renvoie une nouvelle instance.
 * Le constructeur compact vérifie les invariants : impossible de créer une tâche invalide,
 * quel que soit le chemin (service, mapper, test...).
 */
public record Task(
        Long id,
        String tenantId,
        Long projectId,
        String title,
        String description,
        LocalDate dueDate,
        boolean completed,
        Instant createdOn
) {

    public Task {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("A task must have a title");
        }
    }

    /** Nouvelle tâche, pas encore enregistrée (pas d'id) et pas terminée. */
    public static Task create(String tenantId, Long projectId, String title, String description,
                              LocalDate dueDate, Instant now) {
        return new Task(null, tenantId, projectId, title, description, dueDate, false, now);
    }

    /**
     * En retard = pas terminée ET échéance strictement passée.
     * Une tâche sans échéance n'est jamais en retard ; une tâche terminée non plus.
     * (La même règle est écrite en JPQL dans TaskRepository.countOverdueTasksByTenantId :
     * compter en base évite de charger toutes les tâches en mémoire.)
     */
    public boolean isOverdue(LocalDate today) {
        return !completed && dueDate != null && dueDate.isBefore(today);
    }

    public Task complete() {
        return new Task(id, tenantId, projectId, title, description, dueDate, true, createdOn);
    }

    /** Nouvelle version des champs modifiables ; id, tenant, projet et date de création ne bougent pas. */
    public Task withDetails(String newTitle, String newDescription, LocalDate newDueDate, boolean newCompleted) {
        return new Task(id, tenantId, projectId, newTitle, newDescription, newDueDate, newCompleted, createdOn);
    }
}
