package com.ecommerce.backend.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderItemRequest(
        @NotNull(message = "productId is required")
        Long productId,

        @Min(value = 1, message = "quantity must be greater than 0")
        int quantity,

        /** Taille choisie par le client, obligatoire si le produit en propose. */
        @Size(max = 20, message = "size must be at most 20 characters")
        String size,

        /** Nom de la couleur choisie, obligatoire si le produit en propose. */
        @Size(max = 80, message = "color must be at most 80 characters")
        String color
) {
    /** Article sans declinaison : ni taille ni couleur a choisir. */
    public OrderItemRequest(Long productId, int quantity) {
        this(productId, quantity, null, null);
    }
}
