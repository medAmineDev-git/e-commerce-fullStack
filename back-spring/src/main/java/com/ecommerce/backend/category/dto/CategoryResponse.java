package com.ecommerce.backend.category.dto;

public record CategoryResponse(
        Long id,
        String name,
        String description
) {
}
