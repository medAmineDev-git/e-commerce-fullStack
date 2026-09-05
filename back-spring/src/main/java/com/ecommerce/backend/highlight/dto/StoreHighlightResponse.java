package com.ecommerce.backend.highlight.dto;

public record StoreHighlightResponse(
        Long id,
        String iconKey,
        String label,
        String detail,
        boolean enabled,
        Integer position
) {
}
