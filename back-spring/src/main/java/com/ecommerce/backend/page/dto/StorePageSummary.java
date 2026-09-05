package com.ecommerce.backend.page.dto;

/** Ce dont le pied de page a besoin : un libelle et une adresse. */
public record StorePageSummary(
        String slug,
        String title
) {
}
