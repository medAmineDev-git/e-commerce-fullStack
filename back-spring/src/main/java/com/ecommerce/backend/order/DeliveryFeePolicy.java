package com.ecommerce.backend.order;

import com.ecommerce.backend.store.Store;

import java.math.BigDecimal;

/**
 * Frais de livraison d'une commande : un seul forfait pour toute la commande,
 * quel que soit le nombre d'articles, offert a partir d'un seuil. Montant et
 * seuil sont regles par chaque boutique, dans ses parametres.
 *
 * Le serveur fait foi : le montant envoye par le client n'est jamais repris.
 * La vitrine applique la meme regle, avec les valeurs de la boutique qu'elle
 * recoit, pour l'afficher avant la commande (Front/src/app/core/stores/cart.store.ts).
 */
public final class DeliveryFeePolicy {

    /** Valeurs d'une boutique qui n'a encore rien regle. */
    public static final BigDecimal DEFAULT_FEE = new BigDecimal("6.900");
    public static final BigDecimal DEFAULT_FREE_FROM = new BigDecimal("100.000");

    private DeliveryFeePolicy() {
    }

    public static BigDecimal feeFor(Store store, BigDecimal subtotal) {
        BigDecimal fee = store.getDeliveryFee() == null ? BigDecimal.ZERO : store.getDeliveryFee();
        BigDecimal freeFrom = store.getFreeDeliveryFrom();

        // Le seuil est inclus : « offerte a partir de 100 DT » vaut pour 100 DT.
        boolean offered = freeFrom != null && subtotal.compareTo(freeFrom) >= 0;
        if (subtotal.signum() <= 0 || offered) {
            return BigDecimal.ZERO;
        }
        return fee;
    }
}
