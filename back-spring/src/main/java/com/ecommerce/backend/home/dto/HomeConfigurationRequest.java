package com.ecommerce.backend.home.dto;

import jakarta.validation.constraints.NotBlank;

public record HomeConfigurationRequest(
        @NotBlank(message = "title is required")
        String title,

        @NotBlank(message = "text is required")
        String text,

        /**
         * Facultatif : absent, le texte reste affiche. Un client d API qui
         * ignore ce champ ne doit pas faire disparaitre le bloc sans le vouloir.
         */
        Boolean welcomeEnabled
) {
}
