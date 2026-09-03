package com.ecommerce.backend.product;

import com.ecommerce.backend.product.dto.ProductPageResponse;
import com.ecommerce.backend.product.dto.ProductRequest;
import com.ecommerce.backend.product.dto.ProductResponse;
import com.ecommerce.backend.store.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Toutes les operations sont bornees a une boutique : il n'existe volontairement
 * aucune surcharge sans {@link Store}, pour qu'aucun appelant ne puisse lire ou
 * ecrire hors de son perimetre, meme par erreur.
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            new LinkedHashSet<>(List.of("id", "name", "price", "stockQuantity"));
    private static final Set<String> ALLOWED_SEASONS =
            new LinkedHashSet<>(List.of("Printemps", "Été", "Automne", "Hiver"));

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    public List<ProductResponse> getAllProducts(Store store) {
        return productRepository.findAllByStoreOrderByIdDesc(store).stream()
                .map(productMapper::toResponse)
                .toList();
    }

    public ProductResponse getProductById(Store store, Long id) {
        return productMapper.toResponse(findByIdAndStoreOrThrow(id, store));
    }

    public ProductPageResponse searchProducts(
            Store store,
            String query,
            String category,
            String subcategory,
            String season,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "id";
        String safeSortDirection = "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc";

        Sort.Direction direction = "asc".equalsIgnoreCase(safeSortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));
        String safeQuery = query == null ? "" : query.trim();
        String safeCategory = category == null ? "" : category.trim();
        String safeSubcategory = subcategory == null ? "" : subcategory.trim();
        String safeSeason = season == null ? "" : season.trim();
        if (!safeSeason.isBlank() && !ALLOWED_SEASONS.contains(safeSeason)) {
            safeSeason = "";
        }

        Page<Product> resultPage = safeSeason.isBlank() && safeSubcategory.isBlank()
                ? productRepository.searchProductsByStore(store, safeQuery, safeCategory, pageable)
                : safeSeason.isBlank()
                ? productRepository.searchProductsWithSubcategoryByStore(store, safeQuery, safeCategory, safeSubcategory, pageable)
                : productRepository.searchProductsWithSeasonByStore(store, safeQuery, safeCategory, safeSubcategory, safeSeason, pageable);

        return new ProductPageResponse(
                resultPage.getContent().stream().map(productMapper::toResponse).toList(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages(),
                resultPage.isLast(),
                safeSortBy,
                safeSortDirection,
                safeQuery,
                safeCategory
        );
    }

    @Transactional
    public ProductResponse createProduct(Store store, ProductRequest request) {
        Product product = productMapper.toEntity(request);
        product.setStore(store);
        Product created = productRepository.save(product);
        return productMapper.toResponse(created);
    }

    @Transactional
    public ProductResponse updateProduct(Store store, Long id, ProductRequest request) {
        Product existing = findByIdAndStoreOrThrow(id, store);
        productMapper.updateEntity(existing, request);
        Product updated = productRepository.save(existing);
        return productMapper.toResponse(updated);
    }

    @Transactional
    public void deleteProduct(Store store, Long id) {
        Product existing = findByIdAndStoreOrThrow(id, store);
        productRepository.delete(existing);
    }

    /**
     * Un produit appartenant a une autre boutique est signale introuvable, jamais
     * interdit : le code 403 revelerait son existence.
     */
    private Product findByIdAndStoreOrThrow(Long id, Store store) {
        return productRepository.findByIdAndStore(id, store)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
