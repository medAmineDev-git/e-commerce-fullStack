package com.ecommerce.backend.highlight;

import java.util.List;

/**
 * Bandeau propose a la creation d'une boutique.
 *
 * Ce sont des engagements affiches aux clients, pas des decorations : les
 * delais et les durees restent donc a la main du vendeur, et l'ecran de
 * configuration l'en avertit. Les valeurs de depart reprennent les usages les
 * plus repandus du pret-a-porter en ligne.
 */
public final class DefaultStoreHighlights {

    public record Template(String iconKey, String label, String detail) {
    }

    public static final List<Template> TEMPLATES = List.of(
            new Template(HighlightIcons.DELIVERY, "Livraison en 48 h", "Expédition le jour même"),
            new Template(HighlightIcons.SUPPORT, "Une question ?", "Nous répondons 7j/7"),
            new Template(HighlightIcons.RETURNS, "Retours et échanges", "Sous 14 jours"),
            new Template(HighlightIcons.PAYMENT, "Paiement sécurisé", "À la livraison ou en ligne")
    );

    private DefaultStoreHighlights() {
    }
}
