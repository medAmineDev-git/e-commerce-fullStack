package com.ecommerce.backend.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Sort;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
	Optional<CustomerOrder> findByOrderNumber(String orderNumber);
    List<CustomerOrder> findByPublisherRef(String publisherRef, Sort sort);
}
