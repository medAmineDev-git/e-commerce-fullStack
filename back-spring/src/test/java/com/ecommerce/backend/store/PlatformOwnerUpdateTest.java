package com.ecommerce.backend.store;

import com.ecommerce.backend.auth.AdminUser;
import com.ecommerce.backend.auth.AdminUserRepository;
import com.ecommerce.backend.auth.Role;
import com.ecommerce.backend.category.CategoryRepository;
import com.ecommerce.backend.order.OrderRepository;
import com.ecommerce.backend.product.ProductImageStorageService;
import com.ecommerce.backend.product.ProductRepository;
import com.ecommerce.backend.store.dto.StoreOwnerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({PlatformStoreService.class, PlatformOwnerUpdateTest.Encoder.class})
@ActiveProfiles("test")
@DisplayName("L'exploitant reprend la main sur un compte proprietaire")
class PlatformOwnerUpdateTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class Encoder {
        @org.springframework.context.annotation.Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @MockitoBean
    private ProductImageStorageService imageStorageService;

    @Autowired
    private PlatformStoreService platformStoreService;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Store store;
    private AdminUser owner;

    @BeforeEach
    void setUp() {
        owner = user("alice", "alice@example.com", "motdepasse");
        store = new Store();
        store.setName("NOVA");
        store.setSlug("nova-owner");
        store.setActive(true);
        store.setOwner(owner);
        store = storeRepository.save(store);
    }

    @Test
    void shouldChangeTheUsernameAndEmail() {
        var detail = platformStoreService.updateOwner(
                store.getId(),
                new StoreOwnerRequest("alice-nova", "contact@nova.tn", null),
                "superadmin");

        assertThat(detail.ownerUsername()).isEqualTo("alice-nova");
        assertThat(detail.ownerEmail()).isEqualTo("contact@nova.tn");
    }

    /**
     * Le cas courant : l'exploitant corrige une adresse. Toucher au mot de passe
     * a cette occasion deconnecterait un proprietaire qui n'a rien demande.
     */
    @Test
    void shouldLeaveThePasswordAloneWhenNoneIsGiven() {
        String before = adminUserRepository.findById(owner.getId()).orElseThrow().getPasswordHash();

        platformStoreService.updateOwner(
                store.getId(),
                new StoreOwnerRequest("alice-nova", "contact@nova.tn", null),
                "superadmin");

        String after = adminUserRepository.findById(owner.getId()).orElseThrow().getPasswordHash();
        assertThat(after).isEqualTo(before);
        assertThat(passwordEncoder.matches("motdepasse", after)).isTrue();
    }

    @Test
    void shouldTreatABlankPasswordAsNoChange() {
        platformStoreService.updateOwner(
                store.getId(),
                new StoreOwnerRequest("alice", "alice@example.com", "   "),
                "superadmin");

        String hash = adminUserRepository.findById(owner.getId()).orElseThrow().getPasswordHash();
        assertThat(passwordEncoder.matches("motdepasse", hash)).isTrue();
    }

    /** Sans connaitre l'ancien : c'est tout l'interet de la manoeuvre. */
    @Test
    void shouldReplaceThePasswordWithoutKnowingTheOldOne() {
        platformStoreService.updateOwner(
                store.getId(),
                new StoreOwnerRequest("alice", "alice@example.com", "NouveauMotDePasse1"),
                "superadmin");

        String hash = adminUserRepository.findById(owner.getId()).orElseThrow().getPasswordHash();
        assertThat(passwordEncoder.matches("NouveauMotDePasse1", hash)).isTrue();
        assertThat(passwordEncoder.matches("motdepasse", hash)).isFalse();
    }

    /**
     * La connexion cherche sans tenir compte de la casse, alors que la contrainte
     * d'unicite de la base la respecte : 'Bob' et 'bob' pourraient coexister, et
     * la connexion ne saurait plus lequel servir.
     */
    @Test
    void shouldRefuseAUsernameAlreadyTakenWhateverTheCase() {
        user("bob", "bob@example.com", "secret");

        assertThatThrownBy(() -> platformStoreService.updateOwner(
                store.getId(),
                new StoreOwnerRequest("BOB", "alice@example.com", null),
                "superadmin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nom d'utilisateur");
    }

    @Test
    void shouldRefuseAnEmailAlreadyTaken() {
        user("bob", "bob@example.com", "secret");

        assertThatThrownBy(() -> platformStoreService.updateOwner(
                store.getId(),
                new StoreOwnerRequest("alice", "BOB@example.com", null),
                "superadmin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("e-mail");
    }

    /** Garder ses propres identifiants ne doit pas se heurter a soi-meme. */
    @Test
    void shouldAllowKeepingTheSameIdentifiers() {
        var detail = platformStoreService.updateOwner(
                store.getId(),
                new StoreOwnerRequest("alice", "alice@example.com", null),
                "superadmin");

        assertThat(detail.ownerUsername()).isEqualTo("alice");
    }

    @Test
    void shouldRefuseAStoreWithoutOwner() {
        Store orphan = new Store();
        orphan.setName("Sans proprietaire");
        orphan.setSlug("sans-proprietaire");
        orphan.setActive(true);
        orphan = storeRepository.save(orphan);
        Long orphanId = orphan.getId();

        assertThatThrownBy(() -> platformStoreService.updateOwner(
                orphanId,
                new StoreOwnerRequest("alice", "alice@example.com", null),
                "superadmin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private AdminUser user(String username, String email, String rawPassword) {
        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(Role.STORE_OWNER.authority());
        return adminUserRepository.save(user);
    }
}
