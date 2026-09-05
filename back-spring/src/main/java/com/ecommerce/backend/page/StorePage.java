package com.ecommerce.backend.page;

import com.ecommerce.backend.store.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Page de contenu d'une boutique : mentions legales, livraison, retours...
 *
 * Chaque boutique possede les siennes. Le slug sert d'adresse publique et doit
 * donc rester unique a l'interieur d'une boutique, mais deux boutiques peuvent
 * sans probleme avoir chacune leur page mentions-legales.
 */
@Entity
@Table(name = "store_pages", uniqueConstraints = {
        @UniqueConstraint(name = "idx_store_pages_store_slug_unique", columnNames = {"store_id", "slug"})
})
@Getter
@Setter
@NoArgsConstructor
public class StorePage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 80)
    private String slug;

    @Column(nullable = false, length = 160)
    private String title;

    /**
     * Texte brut, jamais du balisage : il est rendu paragraphe par paragraphe
     * cote vitrine. Accepter du HTML ouvrirait une injection dans une page que
     * n'importe quel exploitant de boutique peut editer.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Ordre d'apparition dans le pied de page. */
    @Column(nullable = false)
    private Integer position = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
