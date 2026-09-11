package com.ecommerce.backend.product;

import com.ecommerce.backend.product.dto.AdminProductResponse;
import com.ecommerce.backend.product.dto.ProductRequest;
import com.ecommerce.backend.product.dto.ProductResponse;
import com.ecommerce.backend.product.dto.ProductColorResponse;
import com.ecommerce.backend.product.dto.ProductServiceTermResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setCategory(blankToNull(request.category()));
        product.setSubcategory(blankToNull(request.subcategory()));
        product.setDescription(blankToNull(request.description()));
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        updateCatalogDetails(product, request);
        return product;
    }

    public void updateEntity(Product existing, ProductRequest request) {
        existing.setName(request.name());
        existing.setCategory(blankToNull(request.category()));
        existing.setSubcategory(blankToNull(request.subcategory()));
        existing.setDescription(blankToNull(request.description()));
        existing.setPrice(request.price());
        existing.setStockQuantity(request.stockQuantity());
        updateCatalogDetails(existing, request);
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
            product.getCategory(),
                product.getSubcategory(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getSku(),
                product.getCompareAtPrice(),
                product.getStatus(),
                List.copyOf(product.getImageUrls()),
                List.copyOf(product.getSizes()),
                List.copyOf(product.getSeasons()),
                product.getColors().stream()
                        .map(color -> new ProductColorResponse(color.getName(), color.getHex()))
                        .toList(),
                product.getServiceTerms().stream()
                        .map(term -> new ProductServiceTermResponse(term.getLabel(), term.getValue()))
                        .toList(),
                product.getSeoTitle(),
                product.getSeoDescription()
        );
    }

    /** Reservee aux routes du back-office : elle porte le prix de gros. */
    public AdminProductResponse toAdminResponse(Product product) {
        return new AdminProductResponse(toResponse(product), product.getWholesalePrice());
    }

    private void updateCatalogDetails(Product product, ProductRequest request) {
        product.setSku(blankToNull(request.sku()));
        product.setCompareAtPrice(request.compareAtPrice());
        product.setWholesalePrice(request.wholesalePrice());
        product.setStatus(request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status());
        product.setSeoTitle(blankToNull(request.seoTitle()));
        product.setSeoDescription(blankToNull(request.seoDescription()));
        product.setImageUrls(new ArrayList<>(request.imageUrls() == null ? List.of() : request.imageUrls()));
        product.setSizes(normalizeSizes(request.sizes()));
        product.setSeasons(new ArrayList<>(request.seasons() == null ? List.of() : request.seasons()));
        product.setColors(new ArrayList<>((request.colors() == null ? List.<com.ecommerce.backend.product.dto.ProductColorRequest>of() : request.colors())
                .stream()
                .map(color -> new ProductColor(color.name(), color.hex()))
                .toList()));
        product.setServiceTerms(resolveServiceTerms(request));
    }

    /**
     * Champ absent : le produit recoit les conditions proposees, celles qui
     * figuraient jusqu ici en dur dans la fiche. Liste vide : le vendeur a
     * retire le bloc, il ne doit pas revenir au prochain enregistrement.
     */
    private List<ProductServiceTerm> resolveServiceTerms(ProductRequest request) {
        if (request.serviceTerms() == null) {
            return DefaultProductServiceTerms.TEMPLATES.stream()
                    .map(template -> new ProductServiceTerm(template.label(), template.value()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        return request.serviceTerms().stream()
                .map(term -> new ProductServiceTerm(term.label().trim(), term.value().trim()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Les tailles sont libres : un vendeur pour enfants ecrit « 4 ans », un
     * chausseur « 38 ». Espaces et doublons sont retires, sans egard a la casse —
     * « m » et « M » proposeraient deux fois la meme taille. L'ordre du vendeur
     * est conserve : c'est celui de la fiche.
     */
    private List<String> normalizeSizes(List<String> sizes) {
        List<String> normalized = new ArrayList<>();
        if (sizes == null) {
            return normalized;
        }

        Set<String> seen = new HashSet<>();
        for (String size : sizes) {
            String trimmed = size == null ? "" : size.trim();
            if (!trimmed.isEmpty() && seen.add(trimmed.toLowerCase(Locale.ROOT))) {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
