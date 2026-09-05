package com.ecommerce.backend.highlight.dto;

import jakarta.validation.constraints.NotNull;

/** Les deux emplacements du bandeau, commandes au niveau de la boutique. */
public record HighlightSettingsRequest(
        @NotNull(message = "topEnabled is required")
        Boolean topEnabled,

        @NotNull(message = "bottomEnabled is required")
        Boolean bottomEnabled
) {
}
