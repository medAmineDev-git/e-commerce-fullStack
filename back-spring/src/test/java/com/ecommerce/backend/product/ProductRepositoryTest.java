package com.ecommerce.backend.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldFindProductsByNameOrDescriptionIgnoringCase() {
        productRepository.save(product("Sneaker Alpha", "Sneakers", "Running urban shoe", "89.90", 10));
        productRepository.save(product("Denim Jacket", "Homme", "Blue vintage jacket", "74.50", 4));
        productRepository.save(product("Cap Basic", "Accessoires", "Minimal everyday cap", "19.00", 40));

        Page<Product> result = productRepository.searchProducts("JACKET", "Homme", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Denim Jacket");
    }

    @Test
    void shouldReturnPagedAndSortedProducts() {
        productRepository.save(product("Item C", "Homme", "desc", "30.00", 2));
        productRepository.save(product("Item A", "Femme", "desc", "10.00", 6));
        productRepository.save(product("Item B", "Sneakers", "desc", "20.00", 3));

        Page<Product> page = productRepository.findAll(
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "price"))
        );

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getPrice()).isEqualByComparingTo("10.00");
        assertThat(page.getContent().get(1).getPrice()).isEqualByComparingTo("20.00");
    }

    private Product product(String name, String category, String description, String price, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setCategory(category);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(stock);
        return product;
    }
}
