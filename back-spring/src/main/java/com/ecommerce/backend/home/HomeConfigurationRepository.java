package com.ecommerce.backend.home;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HomeConfigurationRepository extends JpaRepository<HomeConfiguration, Long> {
    Optional<HomeConfiguration> findByConfigKey(String configKey);
}
