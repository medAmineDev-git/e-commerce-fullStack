package com.ecommerce.backend.order;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String orderNumber) {
        super("Commande introuvable: " + orderNumber);
    }
}
