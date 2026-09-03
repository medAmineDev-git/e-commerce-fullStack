package com.ecommerce.backend.product;

import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
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

    @Autowired
    private StoreRepository storeRepository;

    private Store testStore;

    @BeforeEach
    void setUp() {
        Store store = new Store();
        store.setName("NOVA Test");
        store.setSlug("nova-test");
        store.setActive(true);
        testStore = storeRepository.save(store);
    }

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
        product.setStore(testStore);
        product.setName(name);
        product.setCategory(category);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(stock);
        return product;
    }
}
