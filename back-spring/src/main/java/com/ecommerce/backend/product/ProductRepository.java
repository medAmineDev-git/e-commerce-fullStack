package com.ecommerce.backend.product;

import com.ecommerce.backend.store.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByStoreOrderByIdDesc(Store store);

    Optional<Product> findByIdAndStore(Long id, Store store);

    Optional<Product> findByIdAndStoreId(Long id, Long storeId);

    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name,
            String description,
            Pageable pageable
    );

    @Query("""
        SELECT p
        FROM Product p
        WHERE (:category = '' OR UPPER(p.category) = UPPER(:category))
          AND (
             :query = ''
             OR UPPER(p.name) LIKE UPPER(CONCAT('%', :query, '%'))
             OR UPPER(p.description) LIKE UPPER(CONCAT('%', :query, '%'))
          )
    """)
    Page<Product> searchProducts(
        @Param("query") String query,
        @Param("category") String category,
        Pageable pageable
    );

    @Query("""
        SELECT p FROM Product p
        WHERE (:category = '' OR UPPER(p.category) = UPPER(:category))
          AND (:subcategory = '' OR UPPER(p.subcategory) = UPPER(:subcategory))
          AND (:query = '' OR UPPER(p.name) LIKE UPPER(CONCAT('%', :query, '%'))
              OR UPPER(p.description) LIKE UPPER(CONCAT('%', :query, '%')))
    """)
    Page<Product> searchProductsWithSubcategory(
        @Param("query") String query,
        @Param("category") String category,
        @Param("subcategory") String subcategory,
        Pageable pageable
    );

    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN p.seasons seasons
        WHERE (:category = '' OR UPPER(p.category) = UPPER(:category))
          AND (:subcategory = '' OR UPPER(p.subcategory) = UPPER(:subcategory))
          AND (:season = '' OR UPPER(seasons) = UPPER(:season))
          AND (:query = '' OR UPPER(p.name) LIKE UPPER(CONCAT('%', :query, '%'))
              OR UPPER(p.description) LIKE UPPER(CONCAT('%', :query, '%')))
    """)
    Page<Product> searchProductsWithSeason(
        @Param("query") String query,
        @Param("category") String category,
        @Param("subcategory") String subcategory,
        @Param("season") String season,
        Pageable pageable
    );

    @Query("""
        SELECT p
        FROM Product p
        WHERE p.store = :store
          AND (:category = '' OR UPPER(p.category) = UPPER(:category))
          AND (
             :query = ''
             OR UPPER(p.name) LIKE UPPER(CONCAT('%', :query, '%'))
             OR UPPER(p.description) LIKE UPPER(CONCAT('%', :query, '%'))
          )
    """)
    Page<Product> searchProductsByStore(
        @Param("store") Store store,
        @Param("query") String query,
        @Param("category") String category,
        Pageable pageable
    );

    @Query("""
        SELECT p FROM Product p
        WHERE p.store = :store
          AND (:category = '' OR UPPER(p.category) = UPPER(:category))
          AND (:subcategory = '' OR UPPER(p.subcategory) = UPPER(:subcategory))
          AND (:query = '' OR UPPER(p.name) LIKE UPPER(CONCAT('%', :query, '%'))
              OR UPPER(p.description) LIKE UPPER(CONCAT('%', :query, '%')))
    """)
    Page<Product> searchProductsWithSubcategoryByStore(
        @Param("store") Store store,
        @Param("query") String query,
        @Param("category") String category,
        @Param("subcategory") String subcategory,
        Pageable pageable
    );

    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN p.seasons seasons
        WHERE p.store = :store
          AND (:category = '' OR UPPER(p.category) = UPPER(:category))
          AND (:subcategory = '' OR UPPER(p.subcategory) = UPPER(:subcategory))
          AND (:season = '' OR UPPER(seasons) = UPPER(:season))
          AND (:query = '' OR UPPER(p.name) LIKE UPPER(CONCAT('%', :query, '%'))
              OR UPPER(p.description) LIKE UPPER(CONCAT('%', :query, '%')))
    """)
    Page<Product> searchProductsWithSeasonByStore(
        @Param("store") Store store,
        @Param("query") String query,
        @Param("category") String category,
        @Param("subcategory") String subcategory,
        @Param("season") String season,
        Pageable pageable
    );
}
