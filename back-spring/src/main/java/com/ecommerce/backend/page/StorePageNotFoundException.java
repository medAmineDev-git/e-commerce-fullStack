package com.ecommerce.backend.page;

public class StorePageNotFoundException extends RuntimeException {

    public StorePageNotFoundException(Long id) {
        super("Page introuvable avec l'id " + id);
    }

    public StorePageNotFoundException(String slug) {
        super("Page introuvable : " + slug);
    }
}
