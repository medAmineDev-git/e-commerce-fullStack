package com.ecommerce.backend.home.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HomeConfigurationRequest(
        @NotBlank(message = "title is required")
        String title,

        @NotBlank(message = "text is required")
        String text,

        @NotNull(message = "featuredProductId is required")
        Long featuredProductId
) {
}
