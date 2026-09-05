package com.ecommerce.backend.highlight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoreHighlightRequest(
        @NotBlank(message = "iconKey is required")
        @Size(max = 40, message = "Icon key must not exceed 40 characters")
        String iconKey,

        @NotBlank(message = "label is required")
        @Size(max = 80, message = "Label must not exceed 80 characters")
        String label,

        @Size(max = 160, message = "Detail must not exceed 160 characters")
        String detail,

        Boolean enabled,

        Integer position
) {
}
