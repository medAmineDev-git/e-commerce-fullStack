package com.ecommerce.backend.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull(message = "productId is required")
        Long productId,

        @Min(value = 1, message = "quantity must be greater than 0")
        int quantity
) {
}
