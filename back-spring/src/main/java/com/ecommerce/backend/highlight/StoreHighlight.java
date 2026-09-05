package com.ecommerce.backend.highlight;

import com.ecommerce.backend.store.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Une promesse du bandeau de reassurance : livraison, assistance, retours.
 *
 * L'icone est designee par une cle choisie dans un catalogue ferme
 * ({@link HighlightIcons}), jamais par du balisage : le dessin est livre avec
 * le site, le vendeur choisit lequel.
 */
@Entity
@Table(name = "store_highlights")
@Getter
@Setter
@NoArgsConstructor
public class StoreHighlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "icon_key", nullable = false, length = 40)
    private String iconKey;

    @Column(nullable = false, length = 80)
    private String label;

    /** Seconde ligne, facultative : une precision, pas une phrase. */
    @Column(length = 160)
    private String detail;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private Integer position = 0;
}
