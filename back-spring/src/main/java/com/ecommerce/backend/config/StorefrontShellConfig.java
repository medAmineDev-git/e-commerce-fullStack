package com.ecommerce.backend.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Declare {@link StorefrontShellFilter} hors du perimetre des tranches web.
 *
 * Sous @Component, le filtre etait embarque par les tests @WebMvcTest, qui
 * recensent les beans Filter : il y reclamait le registre des domaines, donc le
 * depot des boutiques, absent d'un contexte sans JPA. Les tranches echouaient
 * alors a demarrer, pour un filtre qu'elles n'exercent pas.
 *
 * Une simple @Configuration n'est pas scannee par ces tranches, ce qui suffit a
 * les laisser tranquilles sans rien desactiver en production.
 */
@Configuration
public class StorefrontShellConfig {

    @Bean
    public FilterRegistrationBean<StorefrontShellFilter> storefrontShellFilter(
            StoreDomainRegistry storeDomains
    ) {
        FilterRegistrationBean<StorefrontShellFilter> registration =
                new FilterRegistrationBean<>(new StorefrontShellFilter(storeDomains));
        registration.addUrlPatterns("/", "/index.html");
        // Avant tout : le choix du fichier se decide sur le nom d'hote, pas sur
        // ce que les autres maillons auraient deja resolu.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
