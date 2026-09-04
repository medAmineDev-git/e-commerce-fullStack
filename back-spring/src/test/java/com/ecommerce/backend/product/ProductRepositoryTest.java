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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 10);

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

    private Page<Product> search(Store store, String category, String size, String color,
                                 BigDecimal minPrice, BigDecimal maxPrice, PageRequest page) {
        return productRepository.search(store, category, null, null, size, color, minPrice, maxPrice, page);
    }

    @Test
    void shouldFilterByCategory() {
        productRepository.save(product(nova, "Sneaker Alpha", "Sneakers", "89.90"));
        productRepository.save(product(nova, "Denim Jacket", "Homme", "74.50"));
        productRepository.save(product(nova, "Cap Basic", "Accessoires", "19.00"));

        Page<Product> result = search(nova, "Homme", null, null, null, null, FIRST_PAGE);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Denim Jacket");
    }

    @Test
    void shouldReturnPagedAndSortedProducts() {
        productRepository.save(product(nova, "Item C", "Homme", "30.00"));
        productRepository.save(product(nova, "Item A", "Femme", "10.00"));
        productRepository.save(product(nova, "Item B", "Sneakers", "20.00"));

        Page<Product> page = search(nova, null, null, null, null, null,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "price")));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getPrice()).isEqualByComparingTo("10.00");
        assertThat(page.getContent().get(1).getPrice()).isEqualByComparingTo("20.00");
    }

    @Test
    void searchShouldIgnoreProductsOfOtherStores() {
        productRepository.save(product(nova, "Denim Jacket", "Homme", "74.50"));
        productRepository.save(product(atelier, "Denim Jacket", "Homme", "79.50"));

        Page<Product> result = search(nova, "Homme", null, null, null, null, FIRST_PAGE);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getStore().getSlug()).isEqualTo("nova-test");
    }

    @Test
    void shouldFilterByPriceRange() {
        productRepository.save(product(nova, "Petit prix", "Homme", "19.00"));
        productRepository.save(product(nova, "Milieu", "Homme", "75.00"));
        productRepository.save(product(nova, "Haut", "Homme", "249.00"));

        assertThat(search(nova, null, null, null, new BigDecimal("50"), new BigDecimal("100"), FIRST_PAGE))
                .extracting(Product::getName)
                .containsExactly("Milieu");

        // Une seule borne suffit : l'autre reste ouverte.
        assertThat(search(nova, null, null, null, new BigDecimal("50"), null, FIRST_PAGE))
                .hasSize(2);
    }

    @Test
    void shouldFilterBySize() {
        Product small = product(nova, "Tee S", "Homme", "25.00");
        small.setSizes(List.of("S", "M"));
        productRepository.save(small);

        Product large = product(nova, "Tee XL", "Homme", "25.00");
        large.setSizes(List.of("XL"));
        productRepository.save(large);

        assertThat(search(nova, null, "M", null, null, null, FIRST_PAGE))
                .extracting(Product::getName)
                .containsExactly("Tee S");
    }

    @Test
    void shouldFilterByColour() {
        Product black = product(nova, "Veste noire", "Homme", "120.00");
        black.setColors(List.of(new ProductColor("Noir", "#000000")));
        productRepository.save(black);

        Product beige = product(nova, "Veste beige", "Homme", "120.00");
        beige.setColors(List.of(new ProductColor("Beige", "#d8cbb8")));
        productRepository.save(beige);

        assertThat(search(nova, null, null, "Noir", null, null, FIRST_PAGE))
                .extracting(Product::getName)
                .containsExactly("Veste noire");
    }

    /** Un produit a plusieurs tailles ne doit pas remonter plusieurs fois. */
    @Test
    void shouldNotDuplicateProductsWithSeveralVariants() {
        Product product = product(nova, "Multi", "Homme", "60.00");
        product.setSizes(List.of("S", "M", "L"));
        product.setColors(List.of(new ProductColor("Noir", "#000000"), new ProductColor("Ecru", "#f2ece1")));
        productRepository.save(product);

        assertThat(search(nova, null, null, null, null, null, FIRST_PAGE)).hasSize(1);
    }

    @Test
    void facetsShouldListWhatTheStoreActuallyHas() {
        Product first = product(nova, "Veste", "Homme", "120.00");
        first.setSizes(List.of("M"));
        first.setColors(List.of(new ProductColor("Noir", "#000000")));
        productRepository.save(first);

        Product second = product(nova, "Robe", "Femme", "80.00");
        second.setSizes(List.of("S"));
        productRepository.save(second);

        // Le catalogue de l'autre boutique ne doit pas nourrir ces facettes.
        Product other = product(atelier, "Trench", "Manteaux", "500.00");
        other.setSizes(List.of("XXL"));
        productRepository.save(other);

        assertThat(productRepository.findDistinctCategories(nova)).containsExactly("Femme", "Homme");
        assertThat(productRepository.findDistinctSizes(nova)).containsExactly("M", "S");
        assertThat(productRepository.findDistinctColorNames(nova)).containsExactly("Noir");
        assertThat(productRepository.findMinPrice(nova)).isEqualByComparingTo("80.00");
        assertThat(productRepository.findMaxPrice(nova)).isEqualByComparingTo("120.00");
    }

    @Test
    void findByIdShouldBeScopedToTheStore() {
        Product atelierProduct = productRepository.save(product(atelier, "Trench", "Manteaux", "249.00"));

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

    private Product product(Store store, String name, String category, String price) {
        Product product = new Product();
        product.setStore(store);
        product.setName(name);
        product.setCategory(category);
        product.setDescription("Description " + name);
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(10);
        return product;
    }
}
