package com.ecommerce.backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Promeut un compte existant au role plateforme, au demarrage.
 *
 * Le profil de production ne cree deliberement aucun compte : des identifiants
 * connus d'avance sur une instance publique seraient une porte ouverte. Restait
 * un probleme pratique — obtenir le premier compte plateforme demandait une
 * requete SQL, donc un acces direct a la base, que les hebergeurs reservent
 * souvent a leurs offres payantes.
 *
 * Cette variable resout le cas : on s'inscrit normalement par le formulaire
 * public, on renseigne son nom d'utilisateur ici, et le redemarrage promeut le
 * compte. Seul quelqu'un qui controle deja la configuration du service peut le
 * faire, c'est-a-dire l'exploitant lui-meme.
 *
 * L'operation est idempotente : elle peut rester en place sans effet.
 */
@Component
public class PlatformOwnerBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformOwnerBootstrap.class);

    private final AdminUserRepository adminUserRepository;
    private final String username;

    public PlatformOwnerBootstrap(
            AdminUserRepository adminUserRepository,
            @Value("${app.platform.owner-username:}") String username
    ) {
        this.adminUserRepository = adminUserRepository;
        this.username = username;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username == null || username.isBlank()) {
            return;
        }

        String wanted = username.trim();
        adminUserRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(wanted, wanted)
                .ifPresentOrElse(this::promote,
                        () -> log.warn("Aucun compte '{}' a promouvoir. Inscrivez-vous d'abord "
                                + "sur /inscription, puis redemarrez le service.", wanted));
    }

    private void promote(AdminUser user) {
        String platformRole = Role.SUPER_ADMIN.authority();

        if (platformRole.equals(user.getRole())) {
            log.info("Le compte '{}' exploite deja la plateforme.", user.getUsername());
            return;
        }

        user.setRole(platformRole);
        adminUserRepository.save(user);

        // Le role est porte par le jeton : il ne changera qu'a la prochaine connexion.
        log.info("Le compte '{}' devient exploitant de la plateforme. "
                + "Deconnectez-vous et reconnectez-vous pour que le changement prenne effet.",
                user.getUsername());
    }
}
