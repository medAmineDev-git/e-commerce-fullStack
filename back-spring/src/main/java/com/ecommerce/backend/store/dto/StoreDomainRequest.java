package com.ecommerce.backend.store.dto;

import jakarta.validation.constraints.Size;

public record StoreDomainRequest(
        @Size(max = 255, message = "Domain must not exceed 255 characters")
        String domain
) {
}
