package com.ecommerce.backend.auth.dto;

public record LoginResponse(
        String accessToken,
        String username,
        String role
) {
}
