package com.ecommerce.backend.product.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ce sur quoi il est possible de filtrer dans cette boutique.
 *
 * Le formulaire de filtres se construit a partir d'ici : proposer une taille
 * absente du catalogue conduirait a un resultat vide sans raison comprehensible
 * pour le visiteur.
 */
public record ProductFacetsResponse(
        List<String> categories,
        List<String> sizes,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
}
