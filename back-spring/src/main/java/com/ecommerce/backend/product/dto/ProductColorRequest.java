package com.ecommerce.backend.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProductColorRequest(
        @NotBlank(message = "color name is required")
        String name,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "color hex must be a valid hexadecimal value")
        String hex
) {
}
