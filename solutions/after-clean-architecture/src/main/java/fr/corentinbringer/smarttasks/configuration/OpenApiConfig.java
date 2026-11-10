package fr.corentinbringer.smarttasks.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Déclare l'authentification Bearer dans la documentation OpenAPI.
 *
 * - @SecurityScheme : décrit COMMENT s'authentifier (header "Authorization: Bearer <JWT>").
 *   Swagger UI affiche alors un bouton "Authorize" pour coller le token.
 * - security = @SecurityRequirement : applique ce schéma à TOUS les endpoints,
 *   donc "Try it out" envoie le token (sinon : 401).
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "SmartTasks API", version = "1.0"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "ID token Google (copiez-le depuis l'onglet Réseau du navigateur, header Authorization, sans le préfixe Bearer)"
)
public class OpenApiConfig {
}
