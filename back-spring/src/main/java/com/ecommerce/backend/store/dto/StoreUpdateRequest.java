package com.ecommerce.backend.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoreUpdateRequest(
        @NotBlank(message = "Store name is required")
        @Size(max = 120, message = "Store name must not exceed 120 characters")
        String name,

        @Size(max = 1500, message = "Description must not exceed 1500 characters")
        String description,

        @Size(max = 2000, message = "Logo URL must not exceed 2000 characters")
        String logoUrl,

        @Size(max = 2000, message = "Banner URL must not exceed 2000 characters")
        String bannerUrl,

        @Size(max = 40, message = "Phone must not exceed 40 characters")
        String phone,

        @Size(max = 160, message = "Email must not exceed 160 characters")
        String email,

        @Size(max = 500, message = "Address must not exceed 500 characters")
        String address,

        @Size(max = 255, message = "Domain must not exceed 255 characters")
        String domain
) {
}
