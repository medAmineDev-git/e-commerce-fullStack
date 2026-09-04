package com.ecommerce.backend.store.dto;

import java.time.Instant;

/**
 * Fiche complete d'une boutique, pour l'exploitant de la plateforme.
 *
 * Elle porte ce qui permet de decider : qui la tient, depuis quand, et ce
 * qu'elle contient reellement. Une boutique sans produit ni commande depuis
 * des mois ne se traite pas comme une boutique active.
 */
public record StoreDetailResponse(
        Long id,
        String name,
        String slug,
        String description,
        String domain,
        boolean active,
        Instant createdAt,
        Instant updatedAt,

        // Le proprietaire
        String ownerUsername,
        String ownerEmail,
        String ownerRole,

        // Les coordonnees publiques
        String phone,
        String email,
        String address,

        // Ce que la boutique contient
        long productCount,
        long categoryCount,
        long orderCount,
        long storageUsedBytes
) {
}
