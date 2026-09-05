package com.ecommerce.backend.store;

import com.ecommerce.backend.category.CategoryService;
import com.ecommerce.backend.category.dto.CategoryResponse;
import com.ecommerce.backend.home.HomeConfigurationService;
import com.ecommerce.backend.home.dto.HomeConfigurationResponse;
import com.ecommerce.backend.order.OrderService;
import com.ecommerce.backend.order.dto.OrderRequest;
import com.ecommerce.backend.order.dto.OrderResponse;
import com.ecommerce.backend.highlight.StoreHighlightService;
import com.ecommerce.backend.highlight.dto.StoreHighlightsResponse;
import com.ecommerce.backend.page.StorePageService;
import com.ecommerce.backend.page.dto.StorePageResponse;
import com.ecommerce.backend.page.dto.StorePageSummary;
import com.ecommerce.backend.product.ProductService;
import com.ecommerce.backend.product.dto.ProductFacetsResponse;
import com.ecommerce.backend.product.dto.ProductPageResponse;
import com.ecommerce.backend.product.dto.ProductResponse;
import com.ecommerce.backend.store.dto.SlugCheckResponse;
import com.ecommerce.backend.store.dto.StorePublicResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/public/stores")
public class PublicStoreController {

    private final StoreService storeService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final HomeConfigurationService homeConfigurationService;
    private final StorePageService storePageService;
    private final StoreHighlightService storeHighlightService;

    public PublicStoreController(
            StoreService storeService,
            ProductService productService,
            CategoryService categoryService,
            OrderService orderService,
            HomeConfigurationService homeConfigurationService,
            StorePageService storePageService,
            StoreHighlightService storeHighlightService
    ) {
        this.storeService = storeService;
        this.productService = productService;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.homeConfigurationService = homeConfigurationService;
        this.storePageService = storePageService;
        this.storeHighlightService = storeHighlightService;
    }

    @GetMapping("/{slug}")
    public StorePublicResponse getStore(@PathVariable String slug) {
        return storeService.getPublicStoreBySlug(slug);
    }

    /**
     * Disponibilite du slug pendant la saisie du nom de boutique.
     * Route anonyme : elle sert le formulaire d inscription.
     */
    @GetMapping("/slug-check")
    public SlugCheckResponse checkSlug(@RequestParam("name") String name) {
        return storeService.checkSlugAvailability(name);
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

    /** Valeurs de filtrage reellement presentes dans le catalogue de la boutique. */
    @GetMapping("/{slug}/products/facets")
    public ProductFacetsResponse getStoreProductFacets(@PathVariable String slug) {
        Store store = storeService.getStoreEntityBySlug(slug);
        return productService.getFacets(store);
    }

    @GetMapping("/{slug}/products/page")
    public ProductPageResponse searchStoreProducts(
            @PathVariable String slug,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "subcategory", required = false) String subcategory,
            @RequestParam(name = "season", required = false) String season,
            @RequestParam(name = "productSize", required = false) String productSize,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(name = "sortDirection", defaultValue = "desc") String sortDirection
    ) {
        Store store = storeService.getStoreEntityBySlug(slug);
        return productService.searchProducts(
                store, category, subcategory, season, productSize,
                minPrice, maxPrice, page, size, sortBy, sortDirection);
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

    /** Bandeau de reassurance : emplacements actifs et lignes visibles. */
    @GetMapping("/{slug}/highlights")
    public StoreHighlightsResponse getStoreHighlights(@PathVariable String slug) {
        Store store = storeService.getStoreEntityBySlug(slug);
        return storeHighlightService.getVisibleHighlights(store);
    }

    /** Liens du pied de page : libelles et adresses, sans les textes. */
    @GetMapping("/{slug}/pages")
    public List<StorePageSummary> getStorePages(@PathVariable String slug) {
        Store store = storeService.getStoreEntityBySlug(slug);
        return storePageService.getPageSummaries(store);
    }

    @GetMapping("/{slug}/pages/{pageSlug}")
    public StorePageResponse getStorePage(@PathVariable String slug, @PathVariable String pageSlug) {
        Store store = storeService.getStoreEntityBySlug(slug);
        return storePageService.getPageBySlug(store, pageSlug);
    }

    @PostMapping("/{slug}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeStoreOrder(@PathVariable String slug, @Valid @RequestBody OrderRequest request) {
        Store store = storeService.getStoreEntityBySlug(slug);
        return orderService.placeOrder(store, request);
    }
}
