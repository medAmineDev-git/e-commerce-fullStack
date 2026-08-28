package com.ecommerce.backend.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        String orderId,
        String estimatedDelivery,
        BigDecimal total,
        String status,
        List<OrderItemResponse> items
) {
}
