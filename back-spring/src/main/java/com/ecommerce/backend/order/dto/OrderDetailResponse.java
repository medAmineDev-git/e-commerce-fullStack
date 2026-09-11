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
        String publisherRef,
        String status,
        String estimatedDelivery,
        /** Deja inclus dans le total. */
        BigDecimal deliveryFee,
        BigDecimal total,
        List<OrderItemResponse> items
) {
}
