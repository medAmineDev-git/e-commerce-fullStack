package com.ecommerce.backend.category.dto;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        Long parentId,
        String parentName
) {
        public CategoryResponse(Long id, String name, String description) {
                this(id, name, description, null, null);
        }
}
