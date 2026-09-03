package com.ecommerce.backend.store.dto;

public record StorePublicResponse(
        Long id,
        String name,
        String slug,
        String description,
        String logoUrl,
        String bannerUrl,
        String phone,
        String email,
        String address,
        String domain
) {
}
