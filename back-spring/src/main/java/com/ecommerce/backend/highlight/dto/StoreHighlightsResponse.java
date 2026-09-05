package com.ecommerce.backend.highlight.dto;

import java.util.List;

/**
 * Ce que la vitrine demande en une fois : ou afficher le bandeau, et quoi y
 * mettre. Deux appels separes auraient fait clignoter la page entre les deux.
 */
public record StoreHighlightsResponse(
        boolean topEnabled,
        boolean bottomEnabled,
        List<StoreHighlightResponse> items
) {
}
