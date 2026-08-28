package com.ecommerce.backend.order.dto;

import java.math.BigDecimal;

public record OrderSummaryResponse(
        String orderId,
        String customerName,
        String city,
        String paymentMethod,
        String status,
        String estimatedDelivery,
        BigDecimal total
) {
}
