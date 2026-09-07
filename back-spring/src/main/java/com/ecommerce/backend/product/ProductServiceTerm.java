package com.ecommerce.backend.product;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Une ligne du bloc « Livraison et retours » de la fiche produit.
 *
 * Ces conditions etaient ecrites en dur dans le gabarit, identiques pour toutes
 * les boutiques et tous les articles. Elles varient pourtant d un vendeur a l
 * autre, et parfois d un article a l autre : un meuble ne se retourne pas comme
 * un t-shirt.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductServiceTerm {

    /** L intitule : Livraison, Paiement, Retour, Garantie... */
    @Column(name = "term_label", nullable = false, length = 60)
    private String label;

    @Column(name = "term_value", nullable = false, length = 160)
    private String value;
}
