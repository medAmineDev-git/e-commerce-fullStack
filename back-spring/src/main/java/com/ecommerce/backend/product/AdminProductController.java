package com.ecommerce.backend.product;

import com.ecommerce.backend.product.dto.ProductPageResponse;
import com.ecommerce.backend.product.dto.ProductRequest;
import com.ecommerce.backend.product.dto.ProductResponse;
import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion du catalogue par le proprietaire de la boutique.
 * La vitrine passe par /api/public/stores/{slug}/products.
 */
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final StoreContext storeContext;

    public AdminProductController(ProductService productService, StoreContext storeContext) {
        this.productService = productService;
        this.storeContext = storeContext;
    }

    @GetMapping
    public List<ProductResponse> getAllProducts(Authentication authentication) {
        return productService.getAllProducts(storeContext.requireOwnedStore(authentication));
    }

    @GetMapping("/page")
    public ProductPageResponse searchProducts(
            Authentication authentication,
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "category", defaultValue = "") String category,
            @RequestParam(name = "subcategory", defaultValue = "") String subcategory,
            @RequestParam(name = "season", defaultValue = "") String season,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(name = "sortDirection", defaultValue = "desc") String sortDirection
    ) {
        Store store = storeContext.requireOwnedStore(authentication);
        return productService.searchProducts(store, query, category, subcategory, season, page, size, sortBy, sortDirection);
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id, Authentication authentication) {
        return productService.getProductById(storeContext.requireOwnedStore(authentication), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request, Authentication authentication) {
        return productService.createProduct(storeContext.requireOwnedStore(authentication), request);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            Authentication authentication
    ) {
        return productService.updateProduct(storeContext.requireOwnedStore(authentication), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id, Authentication authentication) {
        productService.deleteProduct(storeContext.requireOwnedStore(authentication), id);
    }
}
