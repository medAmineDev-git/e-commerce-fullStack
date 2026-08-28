package com.ecommerce.backend.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

public record OrderRequest(
        @NotBlank(message = "customerName is required")
        String customerName,

        @NotBlank(message = "phone is required")
        String phone,

        @NotBlank(message = "city is required")
        String city,

        @NotBlank(message = "address is required")
        String address,

        String note,

        @NotBlank(message = "paymentMethod is required")
        String paymentMethod,

        @NotEmpty(message = "items is required")
        List<@Valid OrderItemRequest> items,

        BigDecimal total
) {
}
