package fr.corentinbringer.smarttasks.project.application.port.out;

import fr.corentinbringer.smarttasks.project.application.model.FileContent;

import java.io.InputStream;

/**
 * Port de sortie vers le stockage objet.
 * Toute erreur technique est signalée par une StorageException (non vérifiée) :
 * le métier ne voit ni les exceptions du SDK AWS, ni IOException, ni les types de Spring Web.
 */
public interface FileStoragePort {

    /** @return la clé (objectKey) attribuée au fichier */
    String uploadFile(FileContent file);

    InputStream downloadFile(String objectKey);

    void deleteFile(String objectKey);
}
