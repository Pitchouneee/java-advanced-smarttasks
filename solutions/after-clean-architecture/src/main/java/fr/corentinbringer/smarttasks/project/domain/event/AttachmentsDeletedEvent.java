package fr.corentinbringer.smarttasks.project.domain.event;

import java.util.List;

/**
 * Événement métier : des pièces jointes ont été supprimées.
 * Les fichiers correspondants doivent être effacés du stockage objet,
 * mais seulement APRÈS le commit de la transaction (voir AttachmentStorageCleanupListener).
 */
public record AttachmentsDeletedEvent(List<String> objectKeys) {}
