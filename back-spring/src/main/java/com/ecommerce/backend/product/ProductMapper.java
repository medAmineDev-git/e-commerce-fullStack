package com.ecommerce.backend.product;

import com.ecommerce.backend.product.dto.ProductRequest;
import com.ecommerce.backend.product.dto.ProductResponse;
import com.ecommerce.backend.product.dto.ProductColorResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setCategory(request.category());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        updateCatalogDetails(product, request);
        return product;
    }

    public void updateEntity(Product existing, ProductRequest request) {
        existing.setName(request.name());
        existing.setCategory(request.category());
        existing.setDescription(request.description());
        existing.setPrice(request.price());
        existing.setStockQuantity(request.stockQuantity());
        updateCatalogDetails(existing, request);
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
            product.getCategory(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getSku(),
                product.getCompareAtPrice(),
                product.getStatus(),
                List.copyOf(product.getImageUrls()),
                List.copyOf(product.getSizes()),
                product.getColors().stream()
                        .map(color -> new ProductColorResponse(color.getName(), color.getHex()))
                        .toList(),
                product.getSeoTitle(),
                product.getSeoDescription()
        );
    }

    private void updateCatalogDetails(Product product, ProductRequest request) {
        product.setSku(blankToNull(request.sku()));
        product.setCompareAtPrice(request.compareAtPrice());
        product.setStatus(request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status());
        product.setSeoTitle(blankToNull(request.seoTitle()));
        product.setSeoDescription(blankToNull(request.seoDescription()));
        product.setImageUrls(new ArrayList<>(request.imageUrls() == null ? List.of() : request.imageUrls()));
        product.setSizes(new ArrayList<>(request.sizes() == null ? List.of() : request.sizes()));
        product.setColors(new ArrayList<>((request.colors() == null ? List.<com.ecommerce.backend.product.dto.ProductColorRequest>of() : request.colors())
                .stream()
                .map(color -> new ProductColor(color.name(), color.hex()))
                .toList()));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
