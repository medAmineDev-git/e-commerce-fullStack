package com.ecommerce.backend.product;

import com.ecommerce.backend.store.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductImageStorageServiceTest {

    @TempDir
    Path uploadsRoot;

    private ProductImageStorageService storageService;
    private Store nova;
    private Store atelier;

    @BeforeEach
    void setUp() {
        storageService = new ProductImageStorageService(uploadsRoot.toString());
        nova = store(1L, "nova");
        atelier = store(2L, "atelier");
    }

    @Test
    void shouldStoreImagesUnderTheStoreOwnDirectory() {
        String url = storageService.store(nova, image("visuel.jpg"));

        assertThat(url).startsWith("/uploads/products/1/");
        assertThat(uploadsRoot.resolve("1")).isDirectoryContaining(path -> path.toString().endsWith(".jpg"));
    }

    @Test
    void storedNamesShouldNotBeGuessableFromTheOriginalFilename() {
        String url = storageService.store(nova, image("catalogue-hiver.jpg"));

        assertThat(url).doesNotContain("catalogue-hiver");
    }

    @Test
    void twoStoresShouldNotShareADirectory() {
        storageService.store(nova, image("a.jpg"));
        storageService.store(atelier, image("b.jpg"));

        assertThat(storageService.usedBytes(nova)).isPositive();
        assertThat(uploadsRoot.resolve("1").toFile().list()).hasSize(1);
        assertThat(uploadsRoot.resolve("2").toFile().list()).hasSize(1);
    }

    /** Le point du lot : une boutique ne supprime pas les visuels d'une autre. */
    @Test
    void deletingShouldIgnoreAnImageOwnedByAnotherStore() {
        String atelierUrl = storageService.store(atelier, image("secret.jpg"));

        assertThat(storageService.delete(nova, atelierUrl)).isFalse();
        assertThat(uploadsRoot.resolve("2").toFile().list()).hasSize(1);

        assertThat(storageService.delete(atelier, atelierUrl)).isTrue();
        assertThat(uploadsRoot.resolve("2").toFile().list()).isEmpty();
    }

    @Test
    void deletingShouldRefuseToEscapeTheStoreDirectory() {
        storageService.store(atelier, image("cible.jpg"));

        assertThat(storageService.delete(nova, "/uploads/products/1/../2/cible.jpg")).isFalse();
        assertThat(uploadsRoot.resolve("2").toFile().list()).hasSize(1);
    }

    @Test
    void shouldRejectANonImageFile() {
        MockMultipartFile script = new MockMultipartFile(
                "file", "charge.svg", "image/svg+xml", "<svg onload=\"alert(1)\"/>".getBytes());

        assertThatThrownBy(() -> storageService.store(nova, script))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JPEG, PNG, GIF and WebP");
    }

    @Test
    void shouldRefuseToStoreWithoutAStore() {
        assertThatThrownBy(() -> storageService.store(null, image("a.jpg")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("store is required");
    }

    private MockMultipartFile image(String filename) {
        return new MockMultipartFile("file", filename, "image/jpeg", new byte[]{1, 2, 3, 4});
    }

    private Store store(Long id, String slug) {
        Store store = new Store();
        store.setId(id);
        store.setName(slug);
        store.setSlug(slug);
        return store;
    }
}
