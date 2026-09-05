package com.ecommerce.backend.page;

import com.ecommerce.backend.store.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StorePageRepository extends JpaRepository<StorePage, Long> {

    List<StorePage> findAllByStoreOrderByPositionAscIdAsc(Store store);

    Optional<StorePage> findByIdAndStore(Long id, Store store);

    Optional<StorePage> findByStoreAndSlugIgnoreCase(Store store, String slug);

    boolean existsByStoreAndSlugIgnoreCase(Store store, String slug);

    boolean existsByStoreAndSlugIgnoreCaseAndIdNot(Store store, String slug, Long id);

}
