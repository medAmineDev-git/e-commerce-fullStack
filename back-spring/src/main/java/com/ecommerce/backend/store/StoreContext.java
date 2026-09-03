package com.ecommerce.backend.store;

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
     * @throws StoreNotFoundException si la requete est anonyme, ou si le compte
     *         n'est rattache a aucune boutique. L'absence de perimetre est une
     *         erreur, jamais une invitation a en choisir un par defaut.
     */
    public Store requireOwnedStore(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || ANONYMOUS.equals(authentication.getName())) {
            throw new StoreNotFoundException("aucun utilisateur authentifie");
        }
        return storeService.getStoreOwnedBy(authentication.getName());
    }
}
