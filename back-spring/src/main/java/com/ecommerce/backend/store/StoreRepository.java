package com.ecommerce.backend.store;

import com.ecommerce.backend.auth.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findBySlugIgnoreCase(String slug);

    Optional<Store> findByDomainIgnoreCase(String domain);

    Optional<Store> findFirstByOwner(AdminUser owner);

    Optional<Store> findFirstByOwnerUsernameIgnoreCase(String username);

    List<Store> findAllByOwner(AdminUser owner);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsByDomainIgnoreCase(String domain);
}
