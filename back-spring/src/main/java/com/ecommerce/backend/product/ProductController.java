package com.ecommerce.backend.product;

import com.ecommerce.backend.product.dto.ProductPageResponse;
import com.ecommerce.backend.product.dto.ProductRequest;
import com.ecommerce.backend.product.dto.ProductResponse;
import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final StoreService storeService;

    public ProductController(ProductService productService, StoreService storeService) {
        this.productService = productService;
        this.storeService = storeService;
    }

    @GetMapping
    public List<ProductResponse> getAllProducts(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            Store store = storeService.getStoreForUsername(authentication.getName());
            return productService.getAllProducts(store);
        }
        return productService.getAllProducts();
    }

    @GetMapping("/page")
    public ProductPageResponse searchProducts(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "category", defaultValue = "") String category,
            @RequestParam(name = "subcategory", defaultValue = "") String subcategory,
            @RequestParam(name = "season", defaultValue = "") String season,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(name = "sortDirection", defaultValue = "desc") String sortDirection
    ) {
        return productService.searchProducts(query, category, subcategory, season, page, size, sortBy, sortDirection);
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id, Authentication authentication) {
        if (isAuthenticated(authentication)) {
            Store store = storeService.getStoreForUsername(authentication.getName());
            return productService.getProductById(store, id);
        }
        return productService.getProductById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request, Authentication authentication) {
        if (isAuthenticated(authentication)) {
            Store store = storeService.getStoreForUsername(authentication.getName());
            return productService.createProduct(store, request);
        }
        return productService.createProduct(request);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request, Authentication authentication) {
        if (isAuthenticated(authentication)) {
            Store store = storeService.getStoreForUsername(authentication.getName());
            return productService.updateProduct(store, id, request);
        }
        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id, Authentication authentication) {
        if (isAuthenticated(authentication)) {
            Store store = storeService.getStoreForUsername(authentication.getName());
            productService.deleteProduct(store, id);
            return;
        }
        productService.deleteProduct(id);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser");
    }
}
