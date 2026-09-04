package com.ecommerce.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Ressources statiques : les visuels televerses, et le site lui-meme.
 *
 * Le frontend compile est embarque dans le jar. Un seul service sert donc le
 * site et l'API, sur une seule origine : plus de question d'origine croisee, et
 * un seul deploiement a gerer.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Coque des routes rendues cote client. Ce n'est pas index.html. */
    private static final String CSR_SHELL = "/static/index.csr.html";

    /** Ces prefixes ne doivent jamais tomber sur le repli du site. */
    private static final String[] SERVER_PREFIXES = { "api/", "uploads/" };

    private final String uploadsLocation;

    public WebConfig(@Value("${app.uploads.directory:uploads/products}") String uploadsDirectory) {
        this.uploadsLocation = Paths.get(uploadsDirectory).toAbsolutePath().normalize().toUri().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Les visuels vivent hors du jar, sur le disque.
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations(uploadsLocation)
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .resourceChain(true);

        /*
         * Scripts et feuilles de style portent une empreinte dans leur nom :
         * main-VGYWWZ34.js change de nom des que son contenu change. Ils peuvent
         * donc etre gardes indefiniment, et un deploiement n'a aucun risque de
         * servir un fichier perime.
         *
         * Sans cela, le navigateur retelechargeait plus d'un megaoctet de
         * JavaScript a chaque visite.
         */
        registry.addResourceHandler("/*.js", "/*.css", "/media/**", "/favicon.ico")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .resourceChain(true);

        /*
         * Le reste — index.html, index.csr.html, robots.txt, sitemap.xml — garde
         * un nom fixe. Il doit etre revalide a chaque visite, sinon un
         * deploiement resterait invisible pour les visiteurs deja venus.
         */
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new SinglePageResolver());
    }

    /**
     * Sert le fichier demande s'il existe, sinon la coque de l'application.
     *
     * Les routes comme /boutique/nova n'ont aucun fichier correspondant : sans
     * ce repli, un lien profond ou un rafraichissement renverrait 404.
     *
     * Le repli vise index.csr.html et non index.html : index.html est la page
     * d'accueil du service, figee au prerendu. La renvoyer sur /boutique/nova
     * afficherait le contenu de la page d'accueil avant que l'application ne
     * prenne la main.
     */
    private static final class SinglePageResolver extends PathResourceResolver {

        @Override
        @Nullable
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }

            // Une requete API sans correspondance doit rester un 404 de l'API,
            // et surtout pas renvoyer du HTML a un client qui attend du JSON.
            for (String prefix : SERVER_PREFIXES) {
                if (resourcePath.startsWith(prefix)) {
                    return null;
                }
            }

            ClassPathResource shell = new ClassPathResource(CSR_SHELL);
            return shell.exists() ? shell : null;
        }
    }
}
