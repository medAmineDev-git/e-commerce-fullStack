package com.ecommerce.backend.product;

import com.ecommerce.backend.store.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Chaque requete porte la boutique dans sa signature. Aucune variante non bornee
 * n'est exposee : une requete sans boutique est une fuite en puissance, meme si
 * personne ne l'appelle aujourd'hui.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByStoreOrderByIdDesc(Store store);

    Optional<Product> findByIdAndStore(Long id, Store store);

    long countByStore(Store store);

    /**
     * Recherche unique portant tous les criteres.
     *
     * Les trois requetes precedentes se distinguaient par les seuls criteres
     * qu'elles acceptaient, ce qui obligeait le service a choisir laquelle
     * appeler. Un critere absent vaut ici NULL et ne filtre rien.
     *
     * DISTINCT est indispensable : sans lui, un produit a trois tailles
     * remonterait trois fois a cause des jointures sur les collections.
     *
     * Les parametres passes a UPPER sont explicitement types.
     *
     * Sans le CAST, PostgreSQL ne peut pas deviner le type d'un parametre NULL
     * et le suppose bytea, ce qui fait echouer la requete avec
     * "function upper(bytea) does not exist". H2 laisse passer, d'ou un test
     * vert et une erreur 500 en production.
     */
    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN p.seasons season
        LEFT JOIN p.sizes size
        WHERE p.store = :store
          AND (CAST(:category AS String) IS NULL OR UPPER(p.category) = UPPER(CAST(:category AS String)))
          AND (CAST(:subcategory AS String) IS NULL OR UPPER(p.subcategory) = UPPER(CAST(:subcategory AS String)))
          AND (CAST(:season AS String) IS NULL OR UPPER(season) = UPPER(CAST(:season AS String)))
          AND (CAST(:size AS String) IS NULL OR UPPER(size) = UPPER(CAST(:size AS String)))
          AND (CAST(:minPrice AS BigDecimal) IS NULL OR p.price >= :minPrice)
          AND (CAST(:maxPrice AS BigDecimal) IS NULL OR p.price <= :maxPrice)
    """)
    Page<Product> search(
        @Param("store") Store store,
        @Param("category") String category,
        @Param("subcategory") String subcategory,
        @Param("season") String season,
        @Param("size") String size,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );

    /* ----------------------------------------------------------------
       Facettes : ce qui existe reellement dans le catalogue de la boutique.
       Le formulaire de filtres est construit a partir de ces valeurs, plutot
       que d'une liste ecrite en dur qui proposerait des tailles inexistantes.
       ---------------------------------------------------------------- */

    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.store = :store AND p.category IS NOT NULL ORDER BY p.category")
    List<String> findDistinctCategories(@Param("store") Store store);

    @Query("SELECT DISTINCT size FROM Product p JOIN p.sizes size WHERE p.store = :store ORDER BY size")
    List<String> findDistinctSizes(@Param("store") Store store);

    @Query("SELECT MIN(p.price) FROM Product p WHERE p.store = :store")
    BigDecimal findMinPrice(@Param("store") Store store);

    @Query("SELECT MAX(p.price) FROM Product p WHERE p.store = :store")
    BigDecimal findMaxPrice(@Param("store") Store store);
}
