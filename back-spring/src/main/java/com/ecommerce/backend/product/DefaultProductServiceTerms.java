package com.ecommerce.backend.product;

import java.util.List;

/**
 * Conditions proposees a la creation d un produit.
 *
 * Ce sont exactement celles qui figuraient en dur dans la fiche produit : un
 * article cree sans y toucher s affiche donc comme avant.
 *
 * Comme le bandeau de reassurance, ce sont des engagements lus par les clients.
 * Le vendeur les ajuste article par article.
 */
public final class DefaultProductServiceTerms {

    public record Template(String label, String value) {
    }

    public static final List<Template> TEMPLATES = List.of(
            new Template("Livraison", "48 à 72 heures"),
            new Template("Paiement", "À la livraison ou par virement"),
            new Template("Retour", "Sous 7 jours")
    );

    private DefaultProductServiceTerms() {
    }
}
