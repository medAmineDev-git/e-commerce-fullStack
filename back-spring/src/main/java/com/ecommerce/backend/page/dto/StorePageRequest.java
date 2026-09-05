package com.ecommerce.backend.page.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StorePageRequest(
        @NotBlank(message = "title is required")
        @Size(max = 160, message = "Title must not exceed 160 characters")
        String title,

        /**
         * Facultatif : derive du titre quand il n est pas fourni, pour qu une
         * page creee depuis le back-office n exige pas de connaitre la notion.
         */
        @Size(max = 80, message = "Slug must not exceed 80 characters")
        String slug,

        @NotBlank(message = "content is required")
        String content,

        Integer position
) {
}
