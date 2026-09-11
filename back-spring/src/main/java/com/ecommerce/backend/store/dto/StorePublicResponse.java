package com.ecommerce.backend.store.dto;

import java.math.BigDecimal;

public record StorePublicResponse(
        Long id,
        String name,
        String slug,
        String description,
        String logoUrl,
        String bannerUrl,
        String bannerMobileUrl,
        String phone,
        String email,
        String address,
        String domain,
        /** Le panier affiche les frais avant la commande ; le serveur les recalcule. */
        BigDecimal deliveryFee,
        /** Null : la livraison n'est jamais offerte. */
        BigDecimal freeDeliveryFrom
) {
}
