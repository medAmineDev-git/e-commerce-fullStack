package com.ecommerce.backend.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        String orderId,
        String estimatedDelivery,
        /** Deja inclus dans le total. */
        BigDecimal deliveryFee,
        BigDecimal total,
        String status,
        List<OrderItemResponse> items
) {
}
