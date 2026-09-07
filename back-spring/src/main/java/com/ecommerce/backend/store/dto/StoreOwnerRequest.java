package com.ecommerce.backend.store.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Identifiants du proprietaire d une boutique, modifies par l exploitant.
 *
 * Le mot de passe est facultatif : absent, il reste inchange. C est le cas le
 * plus courant, l exploitant venant souvent corriger une adresse.
 */
public record StoreOwnerRequest(
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 80, message = "Username must be between 3 and 80 characters")
        String username,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 160, message = "Email must not exceed 160 characters")
        String email,

        /** Absent ou vide : le mot de passe actuel est conserve. */
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password
) {
}
