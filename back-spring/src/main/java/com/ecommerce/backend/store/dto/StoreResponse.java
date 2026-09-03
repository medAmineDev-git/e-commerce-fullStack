package com.ecommerce.backend.store.dto;

import java.time.Instant;

public record StoreResponse(
        Long id,
        String name,
        String slug,
        String description,
        String logoUrl,
        String bannerUrl,
        String phone,
        String email,
        String address,
        String domain,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        String ownerUsername
) {
}
