package com.ecommerce.backend.page;

import com.ecommerce.backend.page.dto.StorePageRequest;
import com.ecommerce.backend.page.dto.StorePageResponse;
import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(StorePageService.class)
@ActiveProfiles("test")
@DisplayName("Les pages de contenu appartiennent a leur boutique")
class StorePageServiceTest {

    @Autowired
    private StorePageService storePageService;

    @Autowired
    private StoreRepository storeRepository;

    private Store nova;
    private Store atelier;

    @BeforeEach
    void setUp() {
        nova = store("NOVA Test", "nova-pages");
        atelier = store("Atelier Test", "atelier-pages");
    }

    @Test
    void shouldInstallTheShippedPages() {
        storePageService.installDefaults(nova);

        List<StorePageResponse> pages = storePageService.getPages(nova);

        assertThat(pages).hasSize(DefaultStorePages.TEMPLATES.size());
        assertThat(pages).extracting(StorePageResponse::slug)
                .containsExactly(
                        "mentions-legales",
                        "conditions-generales-de-vente",
                        "livraison",
                        "retours-et-remboursements",
                        "confidentialite");
        assertThat(pages.getFirst().title()).isEqualTo("Mentions légales");
        assertThat(pages.getFirst().content()).isNotBlank();
    }

    /**
     * Ces textes sont lus par les clients de la boutique. Un accent perdu entre
     * le fichier source, la base et la reponse HTTP se voit immediatement, et
     * rien d'autre dans le projet ne surveille cette chaine.
     */
    @Test
    void shouldKeepAccentsThroughPersistence() {
        storePageService.installDefaults(nova);

        assertThat(storePageService.getPageBySlug(nova, "mentions-legales").content())
                .contains("Éditeur du site")
                .contains("Hébergement")
                .contains("Propriété intellectuelle");
        assertThat(storePageService.getPageBySlug(nova, "retours-et-remboursements").content())
                .contains("Délai de rétractation");
    }

    @Test
    void shouldNotDuplicateShippedPagesWhenInstalledTwice() {
        storePageService.installDefaults(nova);
        storePageService.installDefaults(nova);

        assertThat(storePageService.getPages(nova)).hasSize(DefaultStorePages.TEMPLATES.size());
    }

    /**
     * Le cas qui compte : une page supprimee volontairement ne doit pas revenir
     * au demarrage suivant.
     */
    @Test
    void shouldLeaveACuratedStoreAlone() {
        storePageService.installDefaults(nova);
        Long livraisonId = storePageService.getPages(nova).stream()
                .filter(page -> page.slug().equals("livraison"))
                .findFirst()
                .orElseThrow()
                .id();
        storePageService.deletePage(nova, livraisonId);

        boolean installed = storePageService.installDefaultsIfEmpty(nova);

        assertThat(installed).isFalse();
        assertThat(storePageService.getPages(nova)).extracting(StorePageResponse::slug)
                .doesNotContain("livraison");
    }

    @Test
    void shouldInstallShippedPagesInAnEmptyStore() {
        assertThat(storePageService.installDefaultsIfEmpty(nova)).isTrue();
        assertThat(storePageService.getPages(nova)).isNotEmpty();
    }

    @Test
    void shouldDeriveTheAddressFromTheTitle() {
        StorePageResponse created = storePageService.createPage(
                nova, new StorePageRequest("Guide des tailles", null, "Nos coupes.", null));

        assertThat(created.slug()).isEqualTo("guide-des-tailles");
    }

    @Test
    void shouldSuffixAnAddressAlreadyTaken() {
        storePageService.createPage(nova, new StorePageRequest("Livraison", null, "Texte.", null));
        StorePageResponse second = storePageService.createPage(
                nova, new StorePageRequest("Livraison", null, "Autre texte.", null));

        assertThat(second.slug()).isEqualTo("livraison-2");
    }

    /** Deux boutiques peuvent chacune avoir leur page mentions-legales. */
    @Test
    void shouldLetTwoStoresShareTheSameAddress() {
        StorePageResponse first = storePageService.createPage(
                nova, new StorePageRequest("Mentions légales", null, "Texte NOVA.", null));
        StorePageResponse second = storePageService.createPage(
                atelier, new StorePageRequest("Mentions légales", null, "Texte Atelier.", null));

        assertThat(first.slug()).isEqualTo("mentions-legales");
        assertThat(second.slug()).isEqualTo("mentions-legales");
    }

    @Test
    void shouldNotReadAPageFromAnotherStore() {
        StorePageResponse page = storePageService.createPage(
                nova, new StorePageRequest("Livraison", null, "Texte.", null));

        assertThatThrownBy(() -> storePageService.getPageById(atelier, page.id()))
                .isInstanceOf(StorePageNotFoundException.class);
    }

    @Test
    void shouldNotDeleteAPageFromAnotherStore() {
        StorePageResponse page = storePageService.createPage(
                nova, new StorePageRequest("Livraison", null, "Texte.", null));

        assertThatThrownBy(() -> storePageService.deletePage(atelier, page.id()))
                .isInstanceOf(StorePageNotFoundException.class);
        assertThat(storePageService.getPages(nova)).hasSize(1);
    }

    @Test
    void shouldReadAPageByItsAddress() {
        storePageService.createPage(nova, new StorePageRequest("Livraison", null, "Sous 48 h.", null));

        assertThat(storePageService.getPageBySlug(nova, "livraison").content()).isEqualTo("Sous 48 h.");
        assertThatThrownBy(() -> storePageService.getPageBySlug(atelier, "livraison"))
                .isInstanceOf(StorePageNotFoundException.class);
    }

    private Store store(String name, String slug) {
        Store store = new Store();
        store.setName(name);
        store.setSlug(slug);
        store.setActive(true);
        return storeRepository.save(store);
    }
}
