package com.ecommerce.backend.category;

import com.ecommerce.backend.store.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByStoreOrderByIdAsc(Store store);

    Optional<Category> findByIdAndStore(Long id, Store store);

    long countByStore(Store store);

    boolean existsByStoreAndNameIgnoreCase(Store store, String name);

    boolean existsByStoreAndNameIgnoreCaseAndIdNot(Store store, String name, Long id);
}
