package com.ecommerce.backend.category;

import com.ecommerce.backend.category.dto.CategoryRequest;
import com.ecommerce.backend.category.dto.CategoryResponse;
import com.ecommerce.backend.store.StoreContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion des categories par le proprietaire de la boutique.
 * La vitrine passe par /api/public/stores/{slug}/categories.
 */
@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final StoreContext storeContext;

    public AdminCategoryController(CategoryService categoryService, StoreContext storeContext) {
        this.categoryService = categoryService;
        this.storeContext = storeContext;
    }

    @GetMapping
    public List<CategoryResponse> getAllCategories(Authentication authentication) {
        return categoryService.getAllCategories(storeContext.requireOwnedStore(authentication));
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategoryById(@PathVariable Long id, Authentication authentication) {
        return categoryService.getCategoryById(storeContext.requireOwnedStore(authentication), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request, Authentication authentication) {
        return categoryService.createCategory(storeContext.requireOwnedStore(authentication), request);
    }

    @PutMapping("/{id}")
    public CategoryResponse updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request,
            Authentication authentication
    ) {
        return categoryService.updateCategory(storeContext.requireOwnedStore(authentication), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id, Authentication authentication) {
        categoryService.deleteCategory(storeContext.requireOwnedStore(authentication), id);
    }
}
