package com.ecommerce.backend.store.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StoreResponse(
        Long id,
        String name,
        String slug,
        String description,
        String logoUrl,
        String bannerUrl,
        String bannerMobileUrl,
        String phone,
        String email,
        String address,
        String domain,
        BigDecimal deliveryFee,
        BigDecimal freeDeliveryFrom,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        String ownerUsername
) {
}
