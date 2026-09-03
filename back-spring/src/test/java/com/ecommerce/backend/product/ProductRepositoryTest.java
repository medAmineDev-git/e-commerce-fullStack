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

    private Store nova;
    private Store atelier;

    @BeforeEach
    void setUp() {
        nova = store("NOVA Test", "nova-test");
        atelier = store("Atelier Test", "atelier-test");
    }

    @Test
    void shouldFindProductsByNameOrDescriptionIgnoringCase() {
        productRepository.save(product(nova, "Sneaker Alpha", "Sneakers", "Running urban shoe", "89.90", 10));
        productRepository.save(product(nova, "Denim Jacket", "Homme", "Blue vintage jacket", "74.50", 4));
        productRepository.save(product(nova, "Cap Basic", "Accessoires", "Minimal everyday cap", "19.00", 40));

        Page<Product> result = productRepository.searchProductsByStore(
                nova, "JACKET", "Homme", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Denim Jacket");
    }

    @Test
    void shouldReturnPagedAndSortedProducts() {
        productRepository.save(product(nova, "Item C", "Homme", "desc", "30.00", 2));
        productRepository.save(product(nova, "Item A", "Femme", "desc", "10.00", 6));
        productRepository.save(product(nova, "Item B", "Sneakers", "desc", "20.00", 3));

        Page<Product> page = productRepository.searchProductsByStore(
                nova, "", "", PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "price")));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getPrice()).isEqualByComparingTo("10.00");
        assertThat(page.getContent().get(1).getPrice()).isEqualByComparingTo("20.00");
    }

    @Test
    void searchShouldIgnoreProductsOfOtherStores() {
        productRepository.save(product(nova, "Denim Jacket", "Homme", "Blue vintage jacket", "74.50", 4));
        productRepository.save(product(atelier, "Denim Jacket", "Homme", "Blue vintage jacket", "79.50", 2));

        Page<Product> result = productRepository.searchProductsByStore(
                nova, "denim", "", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getStore().getSlug()).isEqualTo("nova-test");
    }

    @Test
    void findByIdShouldBeScopedToTheStore() {
        Product atelierProduct = productRepository.save(
                product(atelier, "Trench", "Manteaux", "desc", "249.00", 1));

        assertThat(productRepository.findByIdAndStore(atelierProduct.getId(), atelier)).isPresent();
        assertThat(productRepository.findByIdAndStore(atelierProduct.getId(), nova)).isEmpty();
    }

    private Store store(String name, String slug) {
        Store store = new Store();
        store.setName(name);
        store.setSlug(slug);
        store.setActive(true);
        return storeRepository.save(store);
    }

    private Product product(Store store, String name, String category, String description, String price, int stock) {
        Product product = new Product();
        product.setStore(store);
        product.setName(name);
        product.setCategory(category);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(stock);
        return product;
    }
}
