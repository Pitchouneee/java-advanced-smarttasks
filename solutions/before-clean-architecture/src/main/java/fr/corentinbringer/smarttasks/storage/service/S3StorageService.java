package fr.corentinbringer.smarttasks.storage.service;

import fr.corentinbringer.smarttasks.configuration.storage.S3Properties;
import fr.corentinbringer.smarttasks.exception.StorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Stockage des pièces jointes via l'API S3 standard (AWS SDK v2).
 * En local, le serveur S3 est RustFS (docker-compose) ; en production, ce pourrait être AWS S3.
 */
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Properties props;

    public String uploadFile(MultipartFile file) {
        String objectKey = UUID.randomUUID().toString();

        try (InputStream is = file.getInputStream()) {
            s3Client.putObject(
                    b -> b.bucket(props.bucket())
                            .key(objectKey)
                            .contentType(file.getContentType()),
                    // La taille doit être connue à l'avance : le SDK l'envoie dans Content-Length
                    RequestBody.fromInputStream(is, file.getSize()));
        } catch (IOException | SdkException e) {
            // On traduit les exceptions du SDK en exception "maison" :
            // les appelants ne dépendent pas d'AWS (ni d'IOException).
            throw new StorageException("Upload failed for object " + objectKey, e);
        }
        return objectKey;
    }

    public InputStream downloadFile(String objectKey) {
        try {
            // ResponseInputStream : flux HTTP ouvert, à fermer par l'appelant (ici, Spring après l'envoi)
            return s3Client.getObject(b -> b.bucket(props.bucket()).key(objectKey));
        } catch (SdkException e) {
            // NoSuchKeyException compris : la base référence un fichier absent, c'est une incohérence → 500
            throw new StorageException("Download failed for object " + objectKey, e);
        }
    }

    public void deleteFile(String objectKey) {
        try {
            s3Client.deleteObject(b -> b.bucket(props.bucket()).key(objectKey));
        } catch (SdkException e) {
            throw new StorageException("Deletion failed for object " + objectKey, e);
        }
    }
}
