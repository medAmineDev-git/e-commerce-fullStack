package com.ecommerce.backend.category;

import com.ecommerce.backend.category.dto.CategoryRequest;
import com.ecommerce.backend.category.dto.CategoryResponse;
import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final StoreService storeService;

    public CategoryController(CategoryService categoryService, StoreService storeService) {
        this.categoryService = categoryService;
        this.storeService = storeService;
    }

    @GetMapping
    public List<CategoryResponse> getAllCategories(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            Store store = storeService.getStoreForUsername(authentication.getName());
            return categoryService.getAllCategories(store);
        }
        return categoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategoryById(@PathVariable Long id, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            Store store = storeService.getStoreForUsername(authentication.getName());
            return categoryService.getCategoryById(store, id);
        }
        return categoryService.getCategoryById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request, Authentication authentication) {
        Store store = resolveStore(authentication);
        return categoryService.createCategory(store, request);
    }

    @PutMapping("/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request, Authentication authentication) {
        Store store = resolveStore(authentication);
        return categoryService.updateCategory(store, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id, Authentication authentication) {
        Store store = resolveStore(authentication);
        categoryService.deleteCategory(store, id);
    }

    private Store resolveStore(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            return storeService.getStoreForUsername(authentication.getName());
        }
        return storeService.getStoreEntityById(1L);
    }
}
