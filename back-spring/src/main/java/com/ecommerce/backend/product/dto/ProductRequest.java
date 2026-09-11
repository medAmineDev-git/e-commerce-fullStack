package com.ecommerce.backend.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        @NotBlank(message = "name is required")
        String name,

        String category,

        String subcategory,

        String description,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "stockQuantity is required")
        @PositiveOrZero(message = "stockQuantity must be greater or equal to 0")
        Integer stockQuantity,

        String sku,

        BigDecimal compareAtPrice,

        /** Facultatif, jamais renvoye a la vitrine. */
        @DecimalMin(value = "0.0", inclusive = false, message = "wholesalePrice must be greater than 0")
        @Digits(integer = 9, fraction = 3, message = "wholesalePrice has at most 3 decimals")
        BigDecimal wholesalePrice,

        String status,

        List<String> imageUrls,

        /**
         * Texte libre : « M », « 38 », « 4 ans », « Taille unique ». La limite
         * de longueur est celle de la colonne ; sans ce controle, une valeur trop
         * longue sortait en erreur serveur au lieu d'un refus explicite.
         */
        @Size(max = MAX_SIZES, message = "a product accepts at most " + MAX_SIZES + " sizes")
        List<
                @NotBlank(message = "size must not be blank")
                @Size(max = MAX_SIZE_LENGTH, message = "size must be at most " + MAX_SIZE_LENGTH + " characters")
                String> sizes,

        List<String> seasons,

        List<ProductColorRequest> colors,

        /**
         * Absent : le produit recoit les conditions proposees.
         * Liste vide : le bloc Livraison et retours ne parait pas sur la fiche.
         *
         * {@code @Valid} est indispensable : sans lui les contraintes portees par
         * chaque ligne ne sont pas evaluees, et un intitule sans valeur
         * s'afficherait sur la fiche suivi de rien.
         */
        @Valid
        List<ProductServiceTermRequest> serviceTerms,

        String seoTitle,

        String seoDescription
) {
    public static final int MAX_SIZES = 30;
    /** Longueur de product_sizes.size_value. */
    public static final int MAX_SIZE_LENGTH = 20;

    public ProductRequest(
            String name,
            String category,
            String description,
            BigDecimal price,
            Integer stockQuantity
    ) {
        this(
                name,
                category,
                "",
                description,
                price,
                stockQuantity,
                "",
                null,
                null,
                "ACTIVE",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                "",
                ""
        );
    }
}
