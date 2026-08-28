package com.ecommerce.backend.product;

import com.ecommerce.backend.product.dto.ProductRequest;
import com.ecommerce.backend.product.dto.ProductPageResponse;
import com.ecommerce.backend.product.dto.ProductResponse;
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

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    // Injection de dependance par constructeur : Spring fournit automatiquement le Repository
    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::toResponse)
                .toList();
    }

    public ProductResponse getProductById(Long id) {
        return productMapper.toResponse(findByIdOrThrow(id));
    }

        public ProductPageResponse searchProducts(
            String query,
            String category,
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
        if (!safeCategory.isBlank() && !ALLOWED_CATEGORIES.contains(safeCategory)) {
            safeCategory = "";
        }

        Page<Product> resultPage = productRepository.searchProducts(safeQuery, safeCategory, pageable);

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
    public ProductResponse createProduct(ProductRequest request) {
        Product created = productRepository.save(productMapper.toEntity(request));
        return productMapper.toResponse(created);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product existing = findByIdOrThrow(id);
        productMapper.updateEntity(existing, request);
        Product updated = productRepository.save(existing);
        return productMapper.toResponse(updated);
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.delete(findByIdOrThrow(id));
    }

    private Product findByIdOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
