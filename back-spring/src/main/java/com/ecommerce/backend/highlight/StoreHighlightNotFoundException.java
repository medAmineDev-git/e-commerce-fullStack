package com.ecommerce.backend.highlight;

public class StoreHighlightNotFoundException extends RuntimeException {

    public StoreHighlightNotFoundException(Long id) {
        super("Argument introuvable avec l'id " + id);
    }
}
