package com.ecommerce.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Sur le domaine d'une boutique, la racine sert la coque de l'application, pas
 * la page d'accueil de la plateforme.
 *
 * Le build produit deux fichiers : index.html, prerendu avec la page de
 * presentation, et index.csr.html, la coque vide. Sans ce filtre, un visiteur
 * de yomna-fashion.com recevait le premier : il voyait « Creez votre boutique
 * en ligne » le temps que l'application demarre, interroge le serveur sur le
 * nom d'hote, puis remplace le contenu par la vitrine.
 *
 * Les liens profonds (/cart, /product/12) tombaient deja sur la coque par le
 * repli du resolveur : seule la racine exacte manquait, l'index existant sous
 * forme de fichier.
 *
 * La page de presentation reste prerendue et servie telle quelle sur le domaine
 * de la plateforme, ou son referencement se joue.
 */
public class StorefrontShellFilter extends OncePerRequestFilter {

    private static final String CSR_SHELL = "static/index.csr.html";

    private final StoreDomainRegistry storeDomains;

    public StorefrontShellFilter(StoreDomainRegistry storeDomains) {
        this.storeDomains = storeDomains;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        if (!isRootPageRequest(request) || !storeDomains.isStoreDomain(hostOf(request))) {
            chain.doFilter(request, response);
            return;
        }

        Resource shell = new ClassPathResource(CSR_SHELL);
        if (!shell.exists()) {
            // Build sans coque : mieux vaut la page d'accueil qu'une erreur.
            chain.doFilter(request, response);
            return;
        }

        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding("UTF-8");
        // Meme regle que l'index : un nom fixe doit etre revalide, sinon un
        // deploiement resterait invisible pour les visiteurs deja venus.
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");

        try (InputStream content = shell.getInputStream()) {
            content.transferTo(response.getOutputStream());
        }
    }

    /** Seule la racine est concernee : tout le reste a deja le bon comportement. */
    private boolean isRootPageRequest(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            return false;
        }

        String path = request.getRequestURI();
        return path == null || path.isEmpty() || "/".equals(path) || "/index.html".equals(path);
    }

    /** Le port n'appartient pas au nom d'hote : localhost:4200 est localhost. */
    private String hostOf(HttpServletRequest request) {
        String host = request.getHeader(HttpHeaders.HOST);
        if (host == null || host.isBlank()) {
            return request.getServerName();
        }
        return host.trim().toLowerCase(Locale.ROOT).split(":")[0];
    }
}
