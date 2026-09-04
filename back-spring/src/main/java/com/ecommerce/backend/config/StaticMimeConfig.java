package com.ecommerce.backend.config;

import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Types MIME des fichiers statiques que le conteneur ne connait pas.
 *
 * L'extension .yaml n'est pas dans la table par defaut : la specification
 * OpenAPI etait donc servie en application/octet-stream. Comme la reponse porte
 * aussi X-Content-Type-Options: nosniff, Swagger UI n'avait pas le droit de
 * deviner le format et refusait de l'interpreter, avec le message trompeur
 * "does not specify a valid version field" alors que le fichier est valide.
 */
@Configuration
public class StaticMimeConfig {

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> yamlMimeMappings() {
        return factory -> {
            MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
            mappings.add("yaml", "application/yaml");
            mappings.add("yml", "application/yaml");
            factory.setMimeMappings(mappings);
        };
    }
}
