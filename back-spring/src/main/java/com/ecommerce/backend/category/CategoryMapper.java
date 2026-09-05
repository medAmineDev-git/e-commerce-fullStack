package com.ecommerce.backend.category;

import com.ecommerce.backend.category.dto.CategoryRequest;
import com.ecommerce.backend.category.dto.CategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.name());
        category.setDescription(blankToNull(request.description()));
        return category;
    }

    public void updateEntity(Category category, CategoryRequest request) {
        category.setName(request.name());
        category.setDescription(blankToNull(request.description()));
    }

    /** Une description vide doit etre absente, pas une chaine vide a afficher. */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
            category.getId(), category.getName(), category.getDescription(),
            category.getParent() == null ? null : category.getParent().getId(),
            category.getParent() == null ? null : category.getParent().getName()
        );
    }
}
