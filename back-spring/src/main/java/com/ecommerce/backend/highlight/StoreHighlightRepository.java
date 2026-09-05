package com.ecommerce.backend.highlight;

import com.ecommerce.backend.store.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreHighlightRepository extends JpaRepository<StoreHighlight, Long> {

    List<StoreHighlight> findAllByStoreOrderByPositionAscIdAsc(Store store);

    List<StoreHighlight> findAllByStoreAndEnabledTrueOrderByPositionAscIdAsc(Store store);

    Optional<StoreHighlight> findByIdAndStore(Long id, Store store);

    long countByStore(Store store);
}
