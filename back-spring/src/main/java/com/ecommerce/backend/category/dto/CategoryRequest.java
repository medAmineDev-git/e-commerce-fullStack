package com.ecommerce.backend.category.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "description is required")
        String description
) {
}
