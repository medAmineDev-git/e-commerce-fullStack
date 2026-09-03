package com.ecommerce.backend.order;

import com.ecommerce.backend.store.Store;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    Optional<CustomerOrder> findByOrderNumberAndStore(String orderNumber, Store store);

    List<CustomerOrder> findAllByStore(Store store, Sort sort);

    List<CustomerOrder> findByStoreAndPublisherRef(Store store, String publisherRef, Sort sort);
}
