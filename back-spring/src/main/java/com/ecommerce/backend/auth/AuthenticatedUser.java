package com.ecommerce.backend.auth;

/**
 * Ce que la requete sait de son appelant, tel que signe dans le jeton.
 * Le perimetre vient d'ici, pas d'une recherche en base sur le nom.
 */
public record AuthenticatedUser(String username, String role, Long storeId, String storeSlug) {

    @Override
    public String toString() {
        return username;
    }
}
