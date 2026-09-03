package com.ecommerce.backend.isolation;

import com.ecommerce.backend.auth.AdminUser;
import com.ecommerce.backend.auth.AdminUserRepository;
import com.ecommerce.backend.auth.JwtService;
import com.ecommerce.backend.auth.Role;
import com.ecommerce.backend.category.Category;
import com.ecommerce.backend.category.CategoryRepository;
import com.ecommerce.backend.category.dto.CategoryRequest;
import com.ecommerce.backend.order.CustomerOrder;
import com.ecommerce.backend.order.OrderRepository;
import com.ecommerce.backend.order.dto.OrderItemRequest;
import com.ecommerce.backend.order.dto.OrderRequest;
import com.ecommerce.backend.order.dto.OrderUpdateRequest;
import com.ecommerce.backend.product.Product;
import com.ecommerce.backend.product.ProductRepository;
import com.ecommerce.backend.product.dto.ProductRequest;
import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matrice d'isolation entre boutiques.
 *
 * Ces tests decrivent la cible du chantier multi-boutique, pas l'etat actuel du code :
 * certains echouent tant que les lots 2 et 3 ne sont pas faits, et c'est voulu.
 * Aucune donnee d'une boutique ne doit etre lisible, modifiable ni supprimable
 * depuis le contexte d'une autre.
 *
 * Convention retenue : une ressource appartenant a une autre boutique repond 404,
 * jamais 403, pour ne pas divulguer son existence.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StoreIsolationTest {

    private static final String NOVA = "nova";
    private static final String ATELIER = "atelier";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Store nova;
    private Store atelier;

    private Product novaProduct;
    private Product atelierProduct;
    private Category novaCategory;
    private Category atelierCategory;
    private CustomerOrder novaOrder;
    private CustomerOrder atelierOrder;

    private String novaToken;
    private String atelierToken;
    private String orphanToken;
    private String platformToken;

    @BeforeEach
    void seedTwoStores() {
        AdminUser novaOwner = adminUser("nova-owner", Role.STORE_OWNER);
        AdminUser atelierOwner = adminUser("atelier-owner", Role.STORE_OWNER);
        AdminUser orphanOwner = adminUser("orphan-owner", Role.STORE_OWNER);
        AdminUser platformOperator = adminUser("platform-operator", Role.SUPER_ADMIN);

        nova = store(NOVA, "NOVA Boutique Urbaine", novaOwner, true);
        atelier = store(ATELIER, "Atelier Rive Gauche", atelierOwner, true);

        novaProduct = product(nova, "Sneaker Urban Pulse", "Sneakers", "89.90", 10);
        atelierProduct = product(atelier, "Trench Rive Gauche", "Manteaux", "249.00", 4);

        novaCategory = category(nova, "Sneakers");
        atelierCategory = category(atelier, "Manteaux");

        novaOrder = order(nova, "CMD-NOVA0001");
        atelierOrder = order(atelier, "CMD-ATEL0001");

        novaToken = jwtService.createAccessToken(novaOwner, nova);
        atelierToken = jwtService.createAccessToken(atelierOwner, atelier);
        orphanToken = jwtService.createAccessToken(orphanOwner, null);
        platformToken = jwtService.createAccessToken(platformOperator, null);
    }

    /**
     * Le perimetre vient du jeton signe. Un jeton fabrique avec la boutique
     * d'autrui ne doit pas suffire : la boutique est relue en verifiant
     * l'appartenance.
     */
    @Test
    void aForgedScopeInTheTokenShouldNotGrantAccess() throws Exception {
        AdminUser novaOwner = adminUserRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase("nova-owner", "nova-owner").orElseThrow();
        String tokenClaimingAnotherStore = jwtService.createAccessToken(novaOwner, atelier);

        mockMvc.perform(get("/api/admin/products").header("Authorization", bearer(tokenClaimingAnotherStore)))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousCallerShouldReceive401NotForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenShouldNotBeAcceptedAsAnAccessToken() throws Exception {
        AdminUser novaOwner = adminUserRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase("nova-owner", "nova-owner").orElseThrow();

        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", bearer(jwtService.createRefreshToken(novaOwner))))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // Le proprietaire ne voit que sa propre boutique
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Un proprietaire ne lit que sa propre boutique")
    class OwnScope {

        @Test
        void adminStoreShouldReturnTheCallerOwnStore() throws Exception {
            mockMvc.perform(get("/api/admin/store").header("Authorization", bearer(novaToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slug").value(NOVA));

            mockMvc.perform(get("/api/admin/store").header("Authorization", bearer(atelierToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slug").value(ATELIER));
        }

        @Test
        void productListShouldOnlyContainOwnProducts() throws Exception {
            mockMvc.perform(get("/api/admin/products").header("Authorization", bearer(novaToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(novaProduct.getId()));
        }

        @Test
        void categoryListShouldOnlyContainOwnCategories() throws Exception {
            mockMvc.perform(get("/api/admin/categories").header("Authorization", bearer(novaToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(novaCategory.getId()));
        }

        @Test
        void orderListShouldOnlyContainOwnOrders() throws Exception {
            mockMvc.perform(get("/api/admin/orders").header("Authorization", bearer(novaToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].orderId").value(novaOrder.getOrderNumber()));
        }
    }

    // ---------------------------------------------------------------
    // Acces croise : jeton d'une boutique, ressource de l'autre
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Une ressource d'une autre boutique est introuvable")
    class CrossTenantAccess {

        @Test
        void readingAnotherStoreProductShouldReturn404() throws Exception {
            mockMvc.perform(get("/api/admin/products/" + atelierProduct.getId())
                            .header("Authorization", bearer(novaToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void updatingAnotherStoreProductShouldReturn404() throws Exception {
            ProductRequest payload = new ProductRequest(
                    "Detourne", "Sneakers", "Tentative de prise de controle", new BigDecimal("1.00"), 1);

            mockMvc.perform(put("/api/admin/products/" + atelierProduct.getId())
                            .header("Authorization", bearer(novaToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deletingAnotherStoreProductShouldReturn404() throws Exception {
            mockMvc.perform(delete("/api/admin/products/" + atelierProduct.getId())
                            .header("Authorization", bearer(novaToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void readingAnotherStoreCategoryShouldReturn404() throws Exception {
            mockMvc.perform(get("/api/admin/categories/" + atelierCategory.getId())
                            .header("Authorization", bearer(novaToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void updatingAnotherStoreCategoryShouldReturn404() throws Exception {
            CategoryRequest payload = new CategoryRequest("Detournee", "Tentative de prise de controle");

            mockMvc.perform(put("/api/admin/categories/" + atelierCategory.getId())
                            .header("Authorization", bearer(novaToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deletingAnotherStoreCategoryShouldReturn404() throws Exception {
            mockMvc.perform(delete("/api/admin/categories/" + atelierCategory.getId())
                            .header("Authorization", bearer(novaToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void readingAnotherStoreOrderShouldReturn404() throws Exception {
            mockMvc.perform(get("/api/admin/orders/" + atelierOrder.getOrderNumber())
                            .header("Authorization", bearer(novaToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void updatingAnotherStoreOrderShouldReturn404() throws Exception {
            OrderUpdateRequest payload = new OrderUpdateRequest("ANNULEE", "Annulation par une autre boutique");

            mockMvc.perform(put("/api/admin/orders/" + atelierOrder.getOrderNumber())
                            .header("Authorization", bearer(novaToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isNotFound());
        }
    }

    // ---------------------------------------------------------------
    // Absence de contexte de boutique : aucune donnee ne doit sortir
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Sans contexte de boutique, rien ne sort")
    class MissingStoreContext {

        @Test
        void adminRoutesShouldRejectAnonymousCallers() throws Exception {
            mockMvc.perform(get("/api/admin/products"))
                    .andExpect(status().is4xxClientError());
            mockMvc.perform(get("/api/admin/categories"))
                    .andExpect(status().is4xxClientError());
            mockMvc.perform(get("/api/admin/orders"))
                    .andExpect(status().is4xxClientError());
            mockMvc.perform(get("/api/admin/home/configuration"))
                    .andExpect(status().is4xxClientError());
        }

        /**
         * Garde-fou de regression. Ces routes servaient le catalogue de toutes les
         * boutiques confondues des que l'appelant etait anonyme. Elles ont ete
         * retirees ; ce test veille a ce qu'elles ne reviennent pas.
         */
        @Test
        void legacyUnscopedRoutesShouldNoLongerAnswer() throws Exception {
            mockMvc.perform(get("/api/products"))
                    .andExpect(status().is4xxClientError());
            mockMvc.perform(get("/api/categories"))
                    .andExpect(status().is4xxClientError());
            mockMvc.perform(get("/api/home/configuration"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        void legacyAnonymousOrderRouteShouldNoLongerAttachToADefaultStore() throws Exception {
            OrderRequest payload = orderRequest(novaProduct.getId());

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        void ownerWithoutStoreShouldNotAdministerTheFirstStore() throws Exception {
            mockMvc.perform(get("/api/admin/products").header("Authorization", bearer(orphanToken)))
                    .andExpect(status().isNotFound());
        }
    }

    // ---------------------------------------------------------------
    // Console plateforme
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("La console plateforme reste hors de portee des boutiques")
    class PlatformConsole {

        @Test
        void storeOwnerCannotListEveryStore() throws Exception {
            mockMvc.perform(get("/api/platform/stores").header("Authorization", bearer(novaToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void platformOperatorCanListEveryStore() throws Exception {
            mockMvc.perform(get("/api/platform/stores").header("Authorization", bearer(platformToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        void storeOwnerCannotDeactivateAnotherStore() throws Exception {
            mockMvc.perform(patch("/api/platform/stores/" + atelier.getId() + "/toggle-active")
                            .header("Authorization", bearer(novaToken)))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------
    // Vitrine publique
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("La vitrine publique ne sert que la boutique demandee")
    class PublicStorefront {

        @Test
        void productListShouldBeScopedToTheSlug() throws Exception {
            mockMvc.perform(get("/api/public/stores/" + NOVA + "/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(novaProduct.getId()));
        }

        @Test
        void readingAnotherStoreProductThroughASlugShouldReturn404() throws Exception {
            mockMvc.perform(get("/api/public/stores/" + NOVA + "/products/" + atelierProduct.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void unknownSlugShouldReturn404() throws Exception {
            mockMvc.perform(get("/api/public/stores/boutique-inexistante/products"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void inactiveStoreShouldReturn404() throws Exception {
            atelier.setActive(false);
            storeRepository.saveAndFlush(atelier);

            mockMvc.perform(get("/api/public/stores/" + ATELIER + "/products"))
                    .andExpect(status().isNotFound());
        }

        /**
         * Le produit d'une autre boutique est introuvable depuis cette vitrine, et
         * non refuse : la reponse ne dit pas qu'il existe ailleurs.
         */
        @Test
        void orderingAProductFromAnotherStoreShouldBeRejected() throws Exception {
            OrderRequest payload = orderRequest(atelierProduct.getId());

            mockMvc.perform(post("/api/public/stores/" + NOVA + "/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isNotFound());
        }
    }

    // ---------------------------------------------------------------
    // Fabriques
    // ---------------------------------------------------------------

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private OrderRequest orderRequest(Long productId) {
        return new OrderRequest(
                "Alice",
                "0600000000",
                "Paris",
                "10 rue Exemple",
                "",
                "cash_on_delivery",
                List.of(new OrderItemRequest(productId, 1)),
                new BigDecimal("1.00")
        );
    }

    private AdminUser adminUser(String username, Role role) {
        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setEmail(username + "@test.local");
        user.setPasswordHash("{noop}not-used");
        user.setRole(role.authority());
        return adminUserRepository.saveAndFlush(user);
    }

    private Store store(String slug, String name, AdminUser owner, boolean active) {
        Store store = new Store();
        store.setSlug(slug);
        store.setName(name);
        store.setOwner(owner);
        store.setActive(active);
        return storeRepository.saveAndFlush(store);
    }

    private Product product(Store store, String name, String category, String price, int stock) {
        Product product = new Product();
        product.setStore(store);
        product.setName(name);
        product.setCategory(category);
        product.setDescription("Description " + name);
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(stock);
        return productRepository.saveAndFlush(product);
    }

    private Category category(Store store, String name) {
        Category category = new Category();
        category.setStore(store);
        category.setName(name);
        category.setDescription("Categorie " + name);
        return categoryRepository.saveAndFlush(category);
    }

    private CustomerOrder order(Store store, String orderNumber) {
        CustomerOrder order = new CustomerOrder();
        order.setStore(store);
        order.setOrderNumber(orderNumber);
        order.setCustomerName("Alice");
        order.setPhone("0600000000");
        order.setCity("Paris");
        order.setAddress("10 rue Exemple");
        order.setPaymentMethod("cash_on_delivery");
        order.setStatus("EN_ATTENTE_VALIDATION_ADMIN");
        order.setEstimatedDelivery(LocalDate.now().plusDays(3));
        order.setTotal(new BigDecimal("89.90"));
        return orderRepository.saveAndFlush(order);
    }
}
