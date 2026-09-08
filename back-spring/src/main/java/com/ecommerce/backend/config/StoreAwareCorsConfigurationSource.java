package com.ecommerce.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Origines autorisees calculees a partir des domaines enregistres, et non figees
 * dans la configuration.
 *
 * Tant que le routage se fait par sous-chemin, seules les origines de base sont
 * utiles. Le jour ou une boutique amene son propre domaine, elle est acceptee
 * sans redeploiement : c'est ce qui rend la greffe du domaine propre indolore.
 *
 * La liste des domaines vient de {@link StoreDomainRegistry}, partagee avec le
 * choix du fichier HTML servi a la racine.
 */
/*
 * Le nom du bean est porteur : http.cors(withDefaults()) cherche un bean
 * nomme exactement "corsConfigurationSource". Sous son nom de classe par
 * defaut, ce composant existait bien mais n'etait jamais consulte, et aucun
 * en-tete Access-Control-Allow-Origin n'etait emis.
 */
@Component("corsConfigurationSource")
public class StoreAwareCorsConfigurationSource implements CorsConfigurationSource {

    private final StoreDomainRegistry storeDomains;
    private final Set<String> staticOrigins;

    public StoreAwareCorsConfigurationSource(
            StoreDomainRegistry storeDomains,
            @Value("${app.cors.allowed-origins:http://localhost:4200}") String configuredOrigins
    ) {
        this.storeDomains = storeDomains;
        this.staticOrigins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .map(origin -> origin.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        String origin = request.getHeader("Origin");
        if (origin != null && isAllowed(origin)) {
            // Origine renvoyee a l'identique plutot qu'un joker : setAllowCredentials
            // interdit "*", et l'echo limite la reponse au demandeur.
            configuration.setAllowedOrigins(List.of(origin));
        } else {
            configuration.setAllowedOrigins(List.copyOf(staticOrigins));
        }

        return configuration;
    }

    private boolean isAllowed(String origin) {
        String normalized = origin.trim().toLowerCase(Locale.ROOT);
        if (staticOrigins.contains(normalized)) {
            return true;
        }

        return storeDomains.isStoreDomain(hostOf(normalized));
    }

    private String hostOf(String origin) {
        try {
            return new URI(origin).getHost();
        } catch (URISyntaxException exception) {
            return null;
        }
    }
}
