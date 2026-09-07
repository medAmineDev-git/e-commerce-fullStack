package com.ecommerce.backend.page;

import java.util.List;

/**
 * Pages livrees a la creation d'une boutique.
 *
 * Un pied de page vide donne l'impression d'une vitrine inachevee, et ces
 * mentions sont attendues par les clients comme par la loi. Le vendeur recoit
 * donc une base redigee, qu'il complete ou supprime a sa guise.
 *
 * Les textes signalent entre crochets ce qui reste a renseigner : une base
 * fausse mais affirmative serait pire qu'une base manifestement a completer.
 * Ce sont des modeles de redaction, pas un conseil juridique.
 */
public final class DefaultStorePages {

    /** Un modele de page : adresse publique, titre, texte. */
    public record Template(String slug, String title, String content) {
    }

    private static final String LEGAL = """
            Cette page rassemble les informations légales de la boutique.

            Éditeur du site
            [Raison sociale], [forme juridique] au capital de [montant] euros.
            Siège social : [adresse complète].
            Immatriculation : [numéro SIRET ou RCS].
            Numéro de TVA intracommunautaire : [numéro].
            Directeur de la publication : [nom].

            Contact
            Adresse électronique : [email]. Téléphone : [numéro].

            Hébergement
            Le site est hébergé par [nom de l'hébergeur], [adresse].

            Propriété intellectuelle
            Les textes, visuels et éléments graphiques présents sur ce site sont
            protégés. Toute reproduction, même partielle, est soumise à
            l'autorisation préalable de l'éditeur.
            """;

    private static final String TERMS = """
            Les présentes conditions régissent les ventes conclues sur ce site.

            Commande
            Toute commande passée sur le site suppose l'acceptation des présentes
            conditions. Le récapitulatif affiché avant validation constitue le
            détail de votre engagement.

            Prix
            Les prix sont indiqués en euros, toutes taxes comprises, hors frais de
            livraison. Ces derniers sont annoncés avant la validation de la
            commande.

            Paiement
            Le règlement s'effectue selon les moyens proposés lors de la commande.
            La commande n'est définitive qu'après encaissement.

            Disponibilité
            Nos offres sont valables tant que les articles figurent au catalogue.
            En cas d'indisponibilité après commande, vous en êtes informé et
            remboursé.

            Droit applicable
            Les présentes conditions sont soumises au droit français.
            """;

    private static final String SHIPPING = """
            Voici comment vos commandes vous parviennent.

            Délais de préparation
            Les commandes sont préparées sous [1 à 2] jours ouvrés après validation.

            Modes et délais de livraison
            Livraison standard : [3 à 5] jours ouvrés, [montant] euros.
            Livraison express : [24 à 48] heures, [montant] euros.
            Retrait en boutique : gratuit, sous [24] heures.

            Frais offerts
            Les frais de livraison sont offerts à partir de [montant] euros
            d'achat.

            Zones desservies
            Nous livrons en [zones desservies]. Pour toute autre destination,
            contactez-nous avant de commander.

            Suivi
            Un lien de suivi vous est adressé par courriel dès l'expédition.
            """;

    private static final String RETURNS = """
            Un article ne vous convient pas ? Voici la marche à suivre.

            Délai de rétractation
            Vous disposez de quatorze jours à compter de la réception pour nous
            informer de votre souhait de retour, sans avoir à vous justifier.

            État des articles
            Les articles sont repris neufs, non portés, non lavés, avec leurs
            étiquettes d'origine.

            Comment retourner
            Écrivez à [email] en indiquant votre numéro de commande. Nous vous
            transmettons la procédure et l'adresse de retour.

            Frais de retour
            Les frais de retour sont [à votre charge / offerts]. En cas d'erreur
            de notre part ou d'article défectueux, ils sont à notre charge.

            Remboursement
            Le remboursement intervient sous quatorze jours après réception du
            retour, sur le moyen de paiement utilisé lors de la commande.
            """;

    private static final String PRIVACY = """
            Cette page explique quelles données nous collectons et pourquoi.

            Données collectées
            Lors d'une commande, nous recueillons votre nom, votre adresse de
            livraison, votre adresse électronique et votre numéro de téléphone.

            Finalité
            Ces données servent à traiter votre commande, à vous livrer et à vous
            répondre. Elles ne sont ni vendues ni cédées à des tiers à des fins
            commerciales.

            Durée de conservation
            Les données liées à une commande sont conservées [durée], le temps
            requis par nos obligations comptables.

            Vos droits
            Vous pouvez demander l'accès à vos données, leur rectification ou leur
            suppression en écrivant à [email].

            Cookies
            Le site dépose les cookies nécessaires à son fonctionnement, notamment
            pour conserver le contenu de votre panier.
            """;

    /** Dans l'ordre ou elles apparaissent dans le pied de page. */
    public static final List<Template> TEMPLATES = List.of(
            new Template("mentions-legales", "Mentions légales", LEGAL),
            new Template("conditions-generales-de-vente", "Conditions générales de vente", TERMS),
            new Template("livraison", "Livraison", SHIPPING),
            new Template("retours-et-remboursements", "Retours et remboursements", RETURNS),
            new Template("confidentialite", "Politique de confidentialité", PRIVACY)
    );

    private DefaultStorePages() {
    }
}
