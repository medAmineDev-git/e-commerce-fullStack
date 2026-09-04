package com.ecommerce.backend.isolation;

import com.ecommerce.backend.product.Product;
import com.ecommerce.backend.product.ProductColor;
import com.ecommerce.backend.product.ProductRepository;
import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La recherche catalogue, executee sur un vrai PostgreSQL.
 *
 * La suite applicative tourne sur H2, qui accepte des requetes que PostgreSQL
 * refuse. C'est arrive : un parametre NULL passe a UPPER() sans type explicite
 * passait sur H2 et echouait en production avec
 * "function upper(bytea) does not exist". Ces tests ferment ce trou.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("La recherche catalogue tient sur PostgreSQL")
class ProductSearchPostgresTest {

    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 20);

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("ecommerce_search_test")
                    .withUsername("ecommerce_app")
                    .withPassword("ecommerce_app");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driverClassName", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreRepository storeRepository;

    private Store store;

    @BeforeEach
    void setUp() {
        Store nova = new Store();
        nova.setName("NOVA");
        nova.setSlug("nova-search-" + System.nanoTime());
        nova.setActive(true);
        store = storeRepository.save(nova);

        Product jacket = product("Veste noire", "Homme", "120.00");
        jacket.setSizes(List.of("M", "L"));
        jacket.setColors(List.of(new ProductColor("Noir", "#000000")));
        productRepository.save(jacket);

        Product dress = product("Robe ecrue", "Femme", "60.00");
        dress.setSizes(List.of("S"));
        dress.setColors(List.of(new ProductColor("Ecru", "#f2ece1")));
        productRepository.save(dress);
    }

    /** Le cas qui echouait : tous les criteres NULL. */
    @Test
    void searchWithoutAnyCriterionShouldNotFail() {
        assertThat(search(null, null, null, null)).hasSize(2);
    }

    @Test
    void shouldFilterByCategory() {
        assertThat(search("Homme", null, null, null))
                .extracting(Product::getName)
                .containsExactly("Veste noire");
    }

    @Test
    void shouldFilterBySize() {
        assertThat(search(null, "M", null, null))
                .extracting(Product::getName)
                .containsExactly("Veste noire");
        assertThat(search(null, "S", null, null))
                .extracting(Product::getName)
                .containsExactly("Robe ecrue");
    }

    @Test
    void shouldFilterByPriceRange() {
        assertThat(search(null, null, new BigDecimal("100"), null)).hasSize(1);
        assertThat(search(null, null, null, new BigDecimal("80"))).hasSize(1);
        assertThat(search(null, null, new BigDecimal("50"), new BigDecimal("200"))).hasSize(2);
    }

    /** La casse ne doit pas empecher de trouver. */
    @Test
    void filtersShouldIgnoreCase() {
        assertThat(search("homme", "m", null, null)).hasSize(1);
    }

    @Test
    void facetsShouldRunOnPostgres() {
        assertThat(productRepository.findDistinctCategories(store)).containsExactly("Femme", "Homme");
        assertThat(productRepository.findDistinctSizes(store)).containsExactly("L", "M", "S");
        assertThat(productRepository.findMinPrice(store)).isEqualByComparingTo("60.00");
        assertThat(productRepository.findMaxPrice(store)).isEqualByComparingTo("120.00");
    }

    private List<Product> search(String category, String size,
                                 BigDecimal minPrice, BigDecimal maxPrice) {
        return productRepository
                .search(store, category, null, null, size, minPrice, maxPrice, FIRST_PAGE)
                .getContent();
    }

    private Product product(String name, String category, String price) {
        Product product = new Product();
        product.setStore(store);
        product.setName(name);
        product.setCategory(category);
        product.setDescription("Description " + name);
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(5);
        return product;
    }
}
