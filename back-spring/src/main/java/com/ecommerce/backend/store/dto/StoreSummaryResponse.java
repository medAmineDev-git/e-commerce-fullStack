package com.ecommerce.backend.store.dto;

import java.time.Instant;

public record StoreSummaryResponse(
        Long id,
        String name,
        String slug,
        String domain,
        boolean active,
        String ownerUsername,
        Instant createdAt
) {
}
