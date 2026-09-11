package com.ecommerce.backend.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        /** Null quand le produit ne proposait pas de taille. */
        String size,
        /** Null quand le produit ne proposait pas de couleur. */
        String color
) {
}
