package com.ecommerce.backend.store;

import com.ecommerce.backend.category.CategoryService;
import com.ecommerce.backend.category.dto.CategoryResponse;
import com.ecommerce.backend.home.HomeConfigurationService;
import com.ecommerce.backend.home.dto.HomeConfigurationResponse;
import com.ecommerce.backend.order.OrderService;
import com.ecommerce.backend.order.dto.OrderRequest;
import com.ecommerce.backend.order.dto.OrderResponse;
import com.ecommerce.backend.product.ProductService;
import com.ecommerce.backend.product.dto.ProductPageResponse;
import com.ecommerce.backend.product.dto.ProductResponse;
import com.ecommerce.backend.store.dto.StorePublicResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/stores")
public class PublicStoreController {

    private final StoreService storeService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final HomeConfigurationService homeConfigurationService;

    public PublicStoreController(
            StoreService storeService,
            ProductService productService,
            CategoryService categoryService,
            OrderService orderService,
            HomeConfigurationService homeConfigurationService
    ) {
        this.storeService = storeService;
        this.productService = productService;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.homeConfigurationService = homeConfigurationService;
    }

    @GetMapping("/{slug}")
    public StorePublicResponse getStore(@PathVariable String slug) {
        return storeService.getPublicStoreBySlug(slug);
    }

    @GetMapping("/resolve")
    public StorePublicResponse resolveStoreByDomain(@RequestParam("domain") String domain) {
        return storeService.resolveStoreByDomain(domain);
    }

    @GetMapping("/{slug}/products")
    public List<ProductResponse> getStoreProducts(@PathVariable String slug) {
        Store store = storeService.getStoreEntityBySlug(slug);
        return productService.getAllProducts(store);
    }

    @GetMapping("/{slug}/products/page")
    public ProductPageResponse searchStoreProducts(
            @PathVariable String slug,
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "category", defaultValue = "") String category,
            @RequestParam(name = "subcategory", defaultValue = "") String subcategory,
            @RequestParam(name = "season", defaultValue = "") String season,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(name = "sortDirection", defaultValue = "desc") String sortDirection
    ) {
        Store store = storeService.getStoreEntityBySlug(slug);
        return productService.searchProducts(store, query, category, subcategory, season, page, size, sortBy, sortDirection);
    }

    @GetMapping("/{slug}/products/{id}")
    public ProductResponse getStoreProductById(@PathVariable String slug, @PathVariable Long id) {
        Store store = storeService.getStoreEntityBySlug(slug);
        return productService.getProductById(store, id);
    }

    @GetMapping("/{slug}/categories")
    public List<CategoryResponse> getStoreCategories(@PathVariable String slug) {
        Store store = storeService.getStoreEntityBySlug(slug);
        return categoryService.getAllCategories(store);
    }

    @GetMapping("/{slug}/categories/{id}")
    public CategoryResponse getStoreCategoryById(@PathVariable String slug, @PathVariable Long id) {
        Store store = storeService.getStoreEntityBySlug(slug);
        return categoryService.getCategoryById(store, id);
    }

    @GetMapping("/{slug}/home")
    public HomeConfigurationResponse getStoreHomeConfiguration(@PathVariable String slug) {
        Store store = storeService.getStoreEntityBySlug(slug);
        return homeConfigurationService.getConfiguration(store);
    }

    @PostMapping("/{slug}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeStoreOrder(@PathVariable String slug, @Valid @RequestBody OrderRequest request) {
        Store store = storeService.getStoreEntityBySlug(slug);
        return orderService.placeOrder(store, request);
    }
}
