package com.ecommerce.backend.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
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
}
