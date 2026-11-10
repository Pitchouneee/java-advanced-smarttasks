package fr.corentinbringer.smarttasks.configuration.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés du stockage objet, lues depuis {@code storage.s3.*} dans application.yaml.
 *
 * Un record + @ConfigurationProperties remplace la série de @Value :
 * les propriétés sont typées, regroupées, et validées au démarrage.
 */
@ConfigurationProperties(prefix = "storage.s3")
public record S3Properties(
        String endpoint,
        String region,
        String accessKey,
        String secretKey,
        String bucket
) {}
