package com.ecommerce.backend.store;

/**
 * Informations de contact proposees a une boutique qui n'en a pas encore.
 *
 * Le pied de page masquait chaque ligne vide : une boutique neuve n'affichait
 * donc aucune section Informations, et sa vitrine paraissait inachevee.
 *
 * L'adresse et le telephone restent entre crochets, comme les textes des pages
 * livrees. Inventer un numero joignable ou une adresse credible aurait mis une
 * affirmation fausse sur une boutique en ligne — et un numero tire au hasard
 * sonne chez quelqu'un. Un gabarit visiblement a completer remplit la place
 * sans rien affirmer.
 *
 * L'adresse electronique fait exception : celle du compte cree a l'inscription
 * est une vraie valeur, deja connue et deja joignable.
 */
public final class DefaultStoreInformation {

    public static final String ADDRESS = "[Adresse de la boutique], [code postal] [ville]";

    public static final String PHONE = "[Votre numéro de téléphone]";

    /**
     * Sert aussi de description pour les moteurs de recherche et d'accroche sur
     * la page d'accueil : elle doit donc se lire, pas se completer. Elle ne
     * promet rien qu'une boutique de vetements ne puisse tenir.
     */
    public static final String DESCRIPTION =
            "Une sélection de pièces choisies une à une, pour une garde-robe qui dure.";

    /**
     * Vrai tant que le bloc de contact n'a jamais ete touche.
     *
     * L'adresse et le telephone sont les deux champs que le vendeur remplit
     * depuis Configuration ; la description, elle, peut venir du formulaire
     * d'inscription et ne dit donc rien de ce bloc.
     *
     * Ce garde-fou existe pour le rattrapage au demarrage : sans lui, un
     * telephone efface volontairement reviendrait a chaque redemarrage.
     */
    public static boolean isUntouched(Store store) {
        return isBlank(store.getAddress()) && isBlank(store.getPhone());
    }

    /**
     * Complete les seuls champs restes vides. Reserve a une boutique qui vient
     * de naitre, ou dont le bloc de contact n'a jamais ete touche.
     *
     * @return true si au moins un champ a ete complete
     */
    public static boolean fillBlanks(Store store) {
        boolean changed = false;

        if (isBlank(store.getAddress())) {
            store.setAddress(ADDRESS);
            changed = true;
        }
        if (isBlank(store.getPhone())) {
            store.setPhone(PHONE);
            changed = true;
        }
        if (isBlank(store.getDescription())) {
            store.setDescription(DESCRIPTION);
            changed = true;
        }

        return changed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private DefaultStoreInformation() {
    }
}
