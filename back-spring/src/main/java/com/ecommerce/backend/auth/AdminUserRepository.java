package com.ecommerce.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    /*
     * Unicite verifiee sans tenir compte de la casse : la connexion cherche deja
     * ainsi, alors que la contrainte d'unicite de la base, elle, distingue les
     * majuscules. Sans ces controles, 'Alice' et 'alice' coexisteraient et la
     * connexion ne saurait plus lequel des deux servir.
     */
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
