package com.ecommerce.backend.category.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank(message = "name is required")
        String name,

        /** Facultative. Le formulaire ne l exigeait deja pas. */
        String description,

        Long parentId
) {
        public CategoryRequest(String name, String description) {
                this(name, description, null);
        }
}
