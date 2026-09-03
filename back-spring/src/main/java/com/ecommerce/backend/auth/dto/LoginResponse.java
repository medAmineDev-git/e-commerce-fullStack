package com.ecommerce.backend.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        String username,
        String role,
        Long storeId,
        String storeSlug
) {
}
