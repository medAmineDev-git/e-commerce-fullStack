package com.ecommerce.backend.store.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Livraison d'une boutique, reglee dans ses parametres.
 *
 * @param deliveryFee      forfait compte une fois par commande ; zero, la livraison est toujours offerte
 * @param freeDeliveryFrom montant d'achat, seuil inclus, a partir duquel elle est offerte ; null, jamais
 */
public record DeliverySettingsRequest(
        @NotNull(message = "deliveryFee is required")
        @DecimalMin(value = "0", message = "deliveryFee must not be negative")
        @Digits(integer = 6, fraction = 3, message = "deliveryFee has at most 3 decimals")
        BigDecimal deliveryFee,

        @DecimalMin(value = "0", inclusive = false, message = "freeDeliveryFrom must be greater than 0")
        @Digits(integer = 6, fraction = 3, message = "freeDeliveryFrom has at most 3 decimals")
        BigDecimal freeDeliveryFrom
) {
}
