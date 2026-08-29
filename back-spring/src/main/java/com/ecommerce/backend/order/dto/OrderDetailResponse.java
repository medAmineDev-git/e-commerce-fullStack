package com.ecommerce.backend.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderDetailResponse(
        String orderId,
        String customerName,
        String phone,
        String city,
        String address,
        String note,
        String paymentMethod,
        String status,
        String estimatedDelivery,
        BigDecimal total,
        List<OrderItemResponse> items
) {
}
