package com.ecommerce.backend.product.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.math.BigDecimal;

/**
 * Produit vu par son vendeur : la fiche publique, plus ce qui ne regarde que lui.
 *
 * Deux types plutot qu'un champ masque au cas par cas : ProductResponse ne peut
 * pas porter le prix de gros, donc aucune route publique ne peut le laisser
 * fuiter par oubli. Le JSON reste plat, identique a la fiche publique augmentee.
 */
public record AdminProductResponse(
        @JsonUnwrapped
        ProductResponse product,

        /** Facultatif ; jamais renvoye par les routes de la vitrine. */
        BigDecimal wholesalePrice
) {
}
