package com.ecommerce.backend.highlight;

import java.util.List;
import java.util.Set;

/**
 * Catalogue ferme des icones disponibles pour le bandeau de reassurance.
 *
 * Le serveur ne connait que des cles ; les dessins vivent dans la vitrine. Ce
 * partage evite de stocker du balisage envoye par un exploitant de boutique, et
 * garde un jeu d'icones coherent d'une boutique a l'autre.
 *
 * Ajouter une icone demande donc deux gestes : une cle ici, un trace dans le
 * composant d'icones cote front. Une cle inconnue est refusee a l'entree plutot
 * que rendue par un carre vide.
 */
public final class HighlightIcons {

    public static final String DELIVERY = "livraison";
    public static final String SUPPORT = "assistance";
    public static final String RETURNS = "retours";
    public static final String PAYMENT = "paiement";
    public static final String PICKUP = "retrait";
    public static final String QUALITY = "qualite";
    public static final String GIFT = "emballage";
    public static final String SECURE = "confiance";

    public static final List<String> KEYS = List.of(
            DELIVERY, SUPPORT, RETURNS, PAYMENT, PICKUP, QUALITY, GIFT, SECURE);

    private static final Set<String> KEY_SET = Set.copyOf(KEYS);

    public static boolean isKnown(String iconKey) {
        return iconKey != null && KEY_SET.contains(iconKey);
    }

    private HighlightIcons() {
    }
}
