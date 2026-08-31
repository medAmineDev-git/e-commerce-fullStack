package com.ecommerce.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);
}
