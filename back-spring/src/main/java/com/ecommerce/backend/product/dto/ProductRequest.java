package com.ecommerce.backend.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        @NotBlank(message = "name is required")
        String name,

        String category,

        String subcategory,

        String description,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "stockQuantity is required")
        @PositiveOrZero(message = "stockQuantity must be greater or equal to 0")
        Integer stockQuantity,

        String sku,

        BigDecimal compareAtPrice,

        String status,

        List<String> imageUrls,

        List<String> sizes,

        List<String> seasons,

        List<ProductColorRequest> colors,

        String seoTitle,

        String seoDescription
) {
    public ProductRequest(
            String name,
            String category,
            String description,
            BigDecimal price,
            Integer stockQuantity
    ) {
        this(
                name,
                category,
                "",
                description,
                price,
                stockQuantity,
                "",
                null,
                "ACTIVE",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "",
                ""
        );
    }
}
