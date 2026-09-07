package com.ecommerce.backend.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductServiceTermRequest(
        @NotBlank(message = "label is required")
        @Size(max = 60, message = "Label must not exceed 60 characters")
        String label,

        @NotBlank(message = "value is required")
        @Size(max = 160, message = "Value must not exceed 160 characters")
        String value
) {
}
