package com.ecommerce.backend.store;

import com.ecommerce.backend.auth.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Point de resolution unique du perimetre d'une requete d'administration.
 *
 * Les controleurs ne decident jamais eux-memes de quelle boutique ils parlent :
 * ils la demandent ici. C'est ce qui remplace l'ancien repli sur la boutique 1,
 * qui laissait un compte sans boutique administrer celle de quelqu'un d'autre.
 */
@Component
public class StoreContext {

    private static final String ANONYMOUS = "anonymousUser";

    private final StoreService storeService;

    public StoreContext(StoreService storeService) {
        this.storeService = storeService;
    }

    /**
     * Boutique administree par le compte authentifie.
     *
     * L'identifiant vient du jeton signe. La boutique est tout de meme relue en
     * base, par cle primaire et en verifiant l'appartenance : un jeton reste
     * valide quelques minutes apres une revocation ou un transfert.
     *
     * @throws StoreNotFoundException si la requete est anonyme, si le compte n'est
     *         rattache a aucune boutique, ou si le jeton designe une boutique qui
     *         ne lui appartient plus. L'absence de perimetre est une erreur,
     *         jamais une invitation a en choisir un par defaut.
     */
    public Store requireOwnedStore(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || ANONYMOUS.equals(authentication.getName())) {
            throw new StoreNotFoundException("aucun utilisateur authentifie");
        }

        if (authentication.getPrincipal() instanceof AuthenticatedUser user && user.storeId() != null) {
            return storeService.getStoreOwnedBy(user.storeId(), user.username());
        }

        // Jeton sans boutique : compte cree avant sa boutique, ou role plateforme.
        return storeService.getStoreOwnedBy(authentication.getName());
    }
}
