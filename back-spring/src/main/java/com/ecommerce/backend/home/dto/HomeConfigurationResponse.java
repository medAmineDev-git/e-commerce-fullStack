package com.ecommerce.backend.home.dto;

public record HomeConfigurationResponse(
        String title,
        String text,
        Long featuredProductId
) {
}
