package com.ecommerce.backend.category;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long id) {
        super("Categorie introuvable avec l'id " + id);
    }
}
