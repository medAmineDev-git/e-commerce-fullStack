package com.ecommerce.backend.order.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderUpdateRequest(
        @NotBlank(message = "status is required")
        String status,

        String note
) {
}
