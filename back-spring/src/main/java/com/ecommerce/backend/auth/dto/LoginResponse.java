package com.ecommerce.backend.auth.dto;

public record LoginResponse(
        String accessToken,
        String username,
        String role,
        String storeSlug
) {
    public LoginResponse(String accessToken, String username, String role) {
        this(accessToken, username, role, null);
    }
}
