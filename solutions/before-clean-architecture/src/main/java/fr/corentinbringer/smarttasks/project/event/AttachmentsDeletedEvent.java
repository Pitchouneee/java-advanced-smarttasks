package fr.corentinbringer.smarttasks.project.event;

import java.util.List;

/**
 * Publié quand des pièces jointes ont été supprimées en base.
 * Les fichiers correspondants doivent être effacés du stockage objet,
 * mais seulement APRÈS le commit de la transaction (voir AttachmentStorageCleanupListener).
 */
public record AttachmentsDeletedEvent(List<String> objectKeys) {}
