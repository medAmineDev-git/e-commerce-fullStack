package com.ecommerce.backend.store;

public class StoreNotFoundException extends RuntimeException {
    public StoreNotFoundException(String identifier) {
        super("Store not found: " + identifier);
    }

    public StoreNotFoundException(Long id) {
        super("Store not found with id: " + id);
    }
}
