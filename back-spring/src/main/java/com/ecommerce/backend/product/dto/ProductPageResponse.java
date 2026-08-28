package com.ecommerce.backend.product.dto;

import java.util.List;

public record ProductPageResponse(
        List<ProductResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last,
        String sortBy,
        String sortDirection,
        String query,
        String category
) {
}
