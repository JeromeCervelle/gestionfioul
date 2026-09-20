package fr.limoges.valadon.gestionfioul.common;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Métadonnées de la documentation Swagger. Le bouton "Authorize" de Swagger UI
 * envoie l'email et le mot de passe en HTTP Basic, comme le fait l'API.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Gestion Fioul",
                version = "1.0",
                description = "Commandes groupées de fioul : campagnes, participations des clients "
                        + "et propositions par paliers des fournisseurs."),
        security = @SecurityRequirement(name = "basicAuth"))
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
public class OpenApiConfig {
}
