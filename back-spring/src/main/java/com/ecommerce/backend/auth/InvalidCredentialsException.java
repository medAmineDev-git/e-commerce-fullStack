package com.ecommerce.backend.auth;

/**
 * Echec d'authentification, distinct d'une requete mal formee : sort en 401,
 * pour que le client sache qu'il doit se reconnecter plutot que corriger sa saisie.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
