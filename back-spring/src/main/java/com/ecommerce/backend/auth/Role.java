package com.ecommerce.backend.auth;

import java.util.Arrays;

/**
 * Les deux seuls roles du systeme.
 *
 * ROLE_ADMIN a disparu : c'etait un role de boutique qui donnait par erreur
 * l'acces a la console plateforme. Les comptes qui le portaient sont devenus
 * proprietaires de boutique par la migration V110.
 */
public enum Role {

    /** Administre une boutique, et une seule. */
    STORE_OWNER,

    /** Exploite la plateforme : inventaire des boutiques, activation, domaines. */
    SUPER_ADMIN;

    private static final String PREFIX = "ROLE_";

    /** Forme attendue par Spring Security et stockee en base. */
    public String authority() {
        return PREFIX + name();
    }

    public static Role fromAuthority(String authority) {
        if (authority == null) {
            throw new IllegalArgumentException("Role is required");
        }
        String normalized = authority.trim().toUpperCase();
        String withoutPrefix = normalized.startsWith(PREFIX)
                ? normalized.substring(PREFIX.length())
                : normalized;

        return Arrays.stream(values())
                .filter(role -> role.name().equals(withoutPrefix))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + authority));
    }
}
