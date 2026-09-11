package com.ecommerce.backend.order;

import java.math.BigDecimal;

/**
 * Frais de livraison d'une commande : un seul forfait pour toute la commande,
 * quel que soit le nombre d'articles, et offert a partir d'un seuil.
 *
 * Le serveur fait foi : le montant envoye par le client n'est jamais repris.
 * La vitrine applique la meme regle pour l'afficher avant la commande
 * (Front/src/app/core/stores/cart.store.ts) — les deux doivent rester alignees
 * tant que ces valeurs ne sont pas reglables par boutique.
 */
public final class DeliveryFeePolicy {

    public static final BigDecimal FLAT_FEE = new BigDecimal("6.900");
    public static final BigDecimal FREE_FROM = new BigDecimal("100.000");

    private DeliveryFeePolicy() {
    }

    public static BigDecimal feeFor(BigDecimal subtotal) {
        if (subtotal.signum() <= 0 || subtotal.compareTo(FREE_FROM) >= 0) {
            return BigDecimal.ZERO;
        }
        return FLAT_FEE;
    }
}
