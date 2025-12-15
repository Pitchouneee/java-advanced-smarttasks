package fr.corentinbringer.smarttasks.minio.service;

import fr.corentinbringer.smarttasks.configuration.minio.MinioConfig;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    public String uploadFile(MultipartFile file) throws Exception {
        String objectName = UUID.randomUUID().toString();

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            return objectName;
        } catch (MinioException e) {
            throw new RuntimeException("MinIO storage failed: " + e.getMessage(), e);
        }
    }

    public InputStream downloadFile(String objectKey) throws Exception {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .build());
        } catch (MinioException e) {
            throw new RuntimeException("MinIO retrieval failed: " + e.getMessage(), e);
        }
    }
}