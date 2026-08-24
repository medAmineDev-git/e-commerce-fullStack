package com.ecommerce.backend.product;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Produit introuvable avec l'id " + id);
    }
}
