package com.ecommerce.backend.page.dto;

import java.time.Instant;

public record StorePageResponse(
        Long id,
        String slug,
        String title,
        String content,
        Integer position,
        Instant updatedAt
) {
}
