package fr.corentinbringer.smarttasks.configuration.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.net.URI;

/**
 * Client S3 générique : il fonctionne avec AWS S3, RustFS, Garage, SeaweedFS...
 * Changer de fournisseur = changer endpoint / identifiants dans application.yaml, pas le code.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Bean
    public S3Client s3Client(S3Properties props) {
        return S3Client.builder()
                // Sans endpointOverride, le SDK appelle AWS (s3.<region>.amazonaws.com)
                .endpointOverride(URI.create(props.endpoint()))
                // Obligatoire pour la signature, même si le serveur local l'ignore
                .region(Region.of(props.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(props.accessKey(), props.secretKey())))
                // http://localhost:9000/<bucket>/<clé> au lieu de http://<bucket>.localhost:9000/<clé>
                .forcePathStyle(true)
                // Depuis 2.30, le SDK ajoute des checksums CRC à chaque requête.
                // Tous les serveurs compatibles S3 ne les gèrent pas : on ne les envoie que si l'API l'exige.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    /**
     * Crée le bucket au démarrage s'il n'existe pas : plus besoin de passer par la console.
     */
    @Bean
    public ApplicationRunner createBucketIfMissing(S3Client s3Client, S3Properties props) {
        return args -> {
            try {
                s3Client.headBucket(b -> b.bucket(props.bucket()));
            } catch (NoSuchBucketException e) {
                s3Client.createBucket(b -> b.bucket(props.bucket()));
                log.info("Bucket '{}' créé", props.bucket());
            }
        };
    }
}
