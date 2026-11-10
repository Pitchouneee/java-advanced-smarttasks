package fr.corentinbringer.smarttasks.storage.service;

import fr.corentinbringer.smarttasks.project.event.AttachmentsDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Supprime les fichiers du stockage objet une fois la suppression en base validée.
 *
 * Pourquoi après le commit ?
 * - Si on effaçait les fichiers AVANT et que la transaction échouait (rollback),
 *   la base pointerait vers des fichiers qui n'existent plus : données corrompues.
 * - En effaçant APRÈS, le pire cas est un fichier orphelin dans le bucket :
 *   il occupe de la place mais ne casse rien, et un batch de nettoyage peut le rattraper.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttachmentStorageCleanupListener {

    private final S3StorageService storageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAttachmentsDeleted(AttachmentsDeletedEvent event) {
        for (String objectKey : event.objectKeys()) {
            try {
                storageService.deleteFile(objectKey);
            } catch (Exception e) {
                // La base est déjà à jour : on journalise sans faire échouer la requête.
                log.warn("Impossible de supprimer l'objet {} du stockage (fichier orphelin)", objectKey, e);
            }
        }
    }
}
