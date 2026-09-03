package com.ecommerce.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterStoreRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 80, message = "Username must be between 3 and 80 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 160, message = "Email must not exceed 160 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 4, max = 100, message = "Password must be at least 4 characters")
        String password,

        @NotBlank(message = "Store name is required")
        @Size(max = 120, message = "Store name must not exceed 120 characters")
        String storeName,

        @Size(max = 80, message = "Slug must not exceed 80 characters")
        String storeSlug,

        @Size(max = 1500, message = "Description must not exceed 1500 characters")
        String description
) {
}
