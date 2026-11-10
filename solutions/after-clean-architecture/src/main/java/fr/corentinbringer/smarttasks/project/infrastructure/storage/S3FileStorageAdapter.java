package fr.corentinbringer.smarttasks.project.infrastructure.storage;

import fr.corentinbringer.smarttasks.configuration.storage.S3Properties;
import fr.corentinbringer.smarttasks.exception.StorageException;
import fr.corentinbringer.smarttasks.project.application.model.FileContent;
import fr.corentinbringer.smarttasks.project.application.port.out.FileStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.InputStream;
import java.util.UUID;

/**
 * Stockage des pièces jointes via l'API S3 standard (AWS SDK v2).
 * En local, le serveur S3 est RustFS (docker-compose) ; en production, ce pourrait être AWS S3.
 */
@Service
@RequiredArgsConstructor
public class S3FileStorageAdapter implements FileStoragePort {

    private final S3Client s3Client;
    private final S3Properties props;

    @Override
    public String uploadFile(FileContent file) {
        String objectKey = UUID.randomUUID().toString();

        try {
            s3Client.putObject(
                    b -> b.bucket(props.bucket())
                            .key(objectKey)
                            .contentType(file.contentType()),
                    // La taille doit être connue à l'avance : le SDK l'envoie dans Content-Length
                    RequestBody.fromInputStream(file.content(), file.size()));
        } catch (SdkException e) {
            // On traduit les exceptions du SDK en exception "maison" :
            // l'application ne dépend pas d'AWS.
            throw new StorageException("Upload failed for object " + objectKey, e);
        }
        return objectKey;
    }

    @Override
    public InputStream downloadFile(String objectKey) {
        try {
            // ResponseInputStream : flux HTTP ouvert, à fermer par l'appelant (ici, Spring après l'envoi)
            return s3Client.getObject(b -> b.bucket(props.bucket()).key(objectKey));
        } catch (SdkException e) {
            // NoSuchKeyException compris : la base référence un fichier absent, c'est une incohérence → 500
            throw new StorageException("Download failed for object " + objectKey, e);
        }
    }

    @Override
    public void deleteFile(String objectKey) {
        try {
            s3Client.deleteObject(b -> b.bucket(props.bucket()).key(objectKey));
        } catch (SdkException e) {
            throw new StorageException("Deletion failed for object " + objectKey, e);
        }
    }
}
