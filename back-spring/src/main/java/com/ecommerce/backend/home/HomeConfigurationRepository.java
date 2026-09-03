package com.ecommerce.backend.home;

import com.ecommerce.backend.store.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HomeConfigurationRepository extends JpaRepository<HomeConfiguration, Long> {
    Optional<HomeConfiguration> findByStoreAndConfigKey(Store store, String configKey);

    Optional<HomeConfiguration> findFirstByStore(Store store);
}
