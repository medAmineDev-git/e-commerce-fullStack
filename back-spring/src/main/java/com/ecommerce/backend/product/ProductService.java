package com.ecommerce.backend.product;

import com.ecommerce.backend.product.dto.ProductPageResponse;
import com.ecommerce.backend.product.dto.ProductRequest;
import com.ecommerce.backend.product.dto.ProductResponse;
import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            new LinkedHashSet<>(List.of("id", "name", "price", "stockQuantity"));
    private static final Set<String> ALLOWED_CATEGORIES =
            new LinkedHashSet<>(List.of("Homme", "Femme", "Sneakers", "Accessoires"));
    private static final Set<String> ALLOWED_SEASONS =
            new LinkedHashSet<>(List.of("Printemps", "Été", "Automne", "Hiver"));

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final StoreRepository storeRepository;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper, StoreRepository storeRepository) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.storeRepository = storeRepository;
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::toResponse)
                .toList();
    }

    public List<ProductResponse> getAllProducts(Store store) {
        return productRepository.findAllByStoreOrderByIdDesc(store).stream()
                .map(productMapper::toResponse)
                .toList();
    }

    public ProductResponse getProductById(Long id) {
        return productMapper.toResponse(findByIdOrThrow(id));
    }

    public ProductResponse getProductById(Store store, Long id) {
        return productMapper.toResponse(findByIdAndStoreOrThrow(id, store));
    }

    public ProductPageResponse searchProducts(
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
        if (!safeCategory.isBlank() && !ALLOWED_CATEGORIES.contains(safeCategory)) {
            safeCategory = "";
        }
        if (!safeSeason.isBlank() && !ALLOWED_SEASONS.contains(safeSeason)) {
            safeSeason = "";
        }

        Page<Product> resultPage = safeSeason.isBlank() && safeSubcategory.isBlank()
                ? productRepository.searchProducts(safeQuery, safeCategory, pageable)
                : safeSeason.isBlank()
                ? productRepository.searchProductsWithSubcategory(safeQuery, safeCategory, safeSubcategory, pageable)
                : productRepository.searchProductsWithSeason(safeQuery, safeCategory, safeSubcategory, safeSeason, pageable);

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

    public ProductPageResponse searchProducts(
            String query,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        return searchProducts(query, "", "", "", page, size, sortBy, sortDirection);
    }

    public ProductPageResponse searchProducts(
            String query,
            String category,
            String subcategory,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        return searchProducts(query, category, subcategory, "", page, size, sortBy, sortDirection);
    }

    public ProductPageResponse searchProducts(
            String query,
            String category,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        return searchProducts(query, category, "", "", page, size, sortBy, sortDirection);
    }

    @Transactional
    public ProductResponse createProduct(Store store, ProductRequest request) {
        Product product = productMapper.toEntity(request);
        product.setStore(store);
        Product created = productRepository.save(product);
        return productMapper.toResponse(created);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        if (storeRepository != null) {
            try {
                storeRepository.findById(1L).ifPresent(product::setStore);
            } catch (Exception ignored) {
            }
        }
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
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product existing = findByIdOrThrow(id);
        productMapper.updateEntity(existing, request);
        Product updated = productRepository.save(existing);
        return productMapper.toResponse(updated);
    }

    @Transactional
    public void deleteProduct(Store store, Long id) {
        Product existing = findByIdAndStoreOrThrow(id, store);
        productRepository.delete(existing);
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.delete(findByIdOrThrow(id));
    }

    private Product findByIdOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Product findByIdAndStoreOrThrow(Long id, Store store) {
        return productRepository.findByIdAndStore(id, store)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Store getDefaultStore() {
        return storeRepository.findById(1L)
                .orElseGet(() -> {
                    Store store = new Store();
                    store.setId(1L);
                    store.setName("NOVA");
                    store.setSlug("nova");
                    store.setActive(true);
                    return storeRepository.save(store);
                });
    }
}
