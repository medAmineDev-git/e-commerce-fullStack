package com.ecommerce.backend.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String category,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        String sku,
        BigDecimal compareAtPrice,
        String status,
        List<String> imageUrls,
        List<String> sizes,
        List<ProductColorResponse> colors,
        String seoTitle,
        String seoDescription
) {
    public ProductResponse(
            Long id,
            String name,
            String category,
            String description,
            BigDecimal price,
            Integer stockQuantity
    ) {
        this(
                id,
                name,
                category,
                description,
                price,
                stockQuantity,
                "",
                null,
                "ACTIVE",
                List.of(),
                List.of(),
                List.of(),
                "",
                ""
        );
    }
}
