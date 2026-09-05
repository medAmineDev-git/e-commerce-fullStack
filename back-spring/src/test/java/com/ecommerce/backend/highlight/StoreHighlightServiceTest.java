package com.ecommerce.backend.highlight;

import com.ecommerce.backend.highlight.dto.HighlightSettingsRequest;
import com.ecommerce.backend.highlight.dto.StoreHighlightRequest;
import com.ecommerce.backend.highlight.dto.StoreHighlightResponse;
import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(StoreHighlightService.class)
@ActiveProfiles("test")
@DisplayName("Le bandeau de reassurance appartient a sa boutique")
class StoreHighlightServiceTest {

    @Autowired
    private StoreHighlightService highlightService;

    @Autowired
    private StoreRepository storeRepository;

    private Store nova;
    private Store atelier;

    @BeforeEach
    void setUp() {
        nova = store("NOVA Test", "nova-highlights");
        atelier = store("Atelier Test", "atelier-highlights");
    }

    @Test
    void shouldInstallTheProposedBanner() {
        assertThat(highlightService.installDefaultsIfEmpty(nova)).isTrue();

        var highlights = highlightService.getHighlights(nova);
        assertThat(highlights.items()).hasSize(DefaultStoreHighlights.TEMPLATES.size());
        assertThat(highlights.items()).extracting(StoreHighlightResponse::label)
                .containsExactly(
                        "Livraison en 48 h",
                        "Une question ?",
                        "Retours et échanges",
                        "Paiement sécurisé");
        assertThat(highlights.topEnabled()).isTrue();
        assertThat(highlights.bottomEnabled()).isFalse();
    }

    /** Une ligne supprimee ne doit pas revenir au redemarrage suivant. */
    @Test
    void shouldLeaveACuratedBannerAlone() {
        highlightService.installDefaultsIfEmpty(nova);
        Long firstId = highlightService.getHighlights(nova).items().getFirst().id();
        highlightService.delete(nova, firstId);

        assertThat(highlightService.installDefaultsIfEmpty(nova)).isFalse();
        assertThat(highlightService.getHighlights(nova).items())
                .hasSize(DefaultStoreHighlights.TEMPLATES.size() - 1);
    }

    /**
     * Le dessin est livre avec la vitrine : une cle hors catalogue produirait un
     * carre vide sur la page, elle est donc refusee a l'entree.
     */
    @Test
    void shouldRejectAnUnknownIcon() {
        assertThatThrownBy(() -> highlightService.create(
                nova, new StoreHighlightRequest("fusee", "Livraison spatiale", null, true, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fusee");
    }

    @Test
    void shouldAcceptEveryIconOfTheCatalogue() {
        for (String iconKey : HighlightIcons.KEYS) {
            assertThat(highlightService.create(
                    nova, new StoreHighlightRequest(iconKey, "Essai", null, true, null)).iconKey())
                    .isEqualTo(iconKey);
        }
    }

    /** Une ligne desactivee reste en base : le vendeur la retrouve intacte. */
    @Test
    void shouldHideADisabledLineFromTheStorefrontButKeepIt() {
        StoreHighlightResponse created = highlightService.create(
                nova, new StoreHighlightRequest(HighlightIcons.DELIVERY, "Livraison", null, true, null));
        highlightService.update(
                nova,
                created.id(),
                new StoreHighlightRequest(HighlightIcons.DELIVERY, "Livraison", null, false, null));

        assertThat(highlightService.getVisibleHighlights(nova).items()).isEmpty();
        assertThat(highlightService.getHighlights(nova).items()).hasSize(1);
        assertThat(highlightService.getHighlights(nova).items().getFirst().enabled()).isFalse();
    }

    @Test
    void shouldStoreBothPlacements() {
        var updated = highlightService.updateSettings(nova, new HighlightSettingsRequest(false, true));

        assertThat(updated.topEnabled()).isFalse();
        assertThat(updated.bottomEnabled()).isTrue();
        assertThat(highlightService.getVisibleHighlights(nova).bottomEnabled()).isTrue();
    }

    @Test
    void shouldNotTouchALineFromAnotherStore() {
        StoreHighlightResponse created = highlightService.create(
                nova, new StoreHighlightRequest(HighlightIcons.DELIVERY, "Livraison", null, true, null));

        assertThatThrownBy(() -> highlightService.delete(atelier, created.id()))
                .isInstanceOf(StoreHighlightNotFoundException.class);
        assertThatThrownBy(() -> highlightService.update(
                atelier,
                created.id(),
                new StoreHighlightRequest(HighlightIcons.SUPPORT, "Pirate", null, true, null)))
                .isInstanceOf(StoreHighlightNotFoundException.class);
        assertThat(highlightService.getHighlights(nova).items()).hasSize(1);
    }

    /** Les emplacements d'une boutique ne commandent pas ceux d'une autre. */
    @Test
    void shouldKeepPlacementsPerStore() {
        highlightService.updateSettings(nova, new HighlightSettingsRequest(false, true));

        assertThat(highlightService.getHighlights(atelier).topEnabled()).isTrue();
        assertThat(highlightService.getHighlights(atelier).bottomEnabled()).isFalse();
    }

    private Store store(String name, String slug) {
        Store store = new Store();
        store.setName(name);
        store.setSlug(slug);
        store.setActive(true);
        return storeRepository.save(store);
    }
}
