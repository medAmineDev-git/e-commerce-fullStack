package com.ecommerce.backend.page;

import com.ecommerce.backend.page.dto.StorePageRequest;
import com.ecommerce.backend.page.dto.StorePageResponse;
import com.ecommerce.backend.page.dto.StorePageSummary;
import com.ecommerce.backend.store.Store;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Pages de contenu, bornees a une boutique.
 *
 * Le proprietaire peut tout modifier et tout supprimer, y compris les pages
 * livrees : ce sont des propositions, pas des obligations de la plateforme.
 */
@Service
@Transactional(readOnly = true)
public class StorePageService {

    private final StorePageRepository storePageRepository;

    public StorePageService(StorePageRepository storePageRepository) {
        this.storePageRepository = storePageRepository;
    }

    public List<StorePageResponse> getPages(Store store) {
        return storePageRepository.findAllByStoreOrderByPositionAscIdAsc(store).stream()
                .map(StorePageService::toResponse)
                .toList();
    }

    /** Ce que le pied de page affiche : pas besoin d'y charger tous les textes. */
    public List<StorePageSummary> getPageSummaries(Store store) {
        return storePageRepository.findAllByStoreOrderByPositionAscIdAsc(store).stream()
                .map(page -> new StorePageSummary(page.getSlug(), page.getTitle()))
                .toList();
    }

    public StorePageResponse getPageById(Store store, Long id) {
        return toResponse(findByIdAndStoreOrThrow(id, store));
    }

    public StorePageResponse getPageBySlug(Store store, String slug) {
        return storePageRepository.findByStoreAndSlugIgnoreCase(store, slug)
                .map(StorePageService::toResponse)
                .orElseThrow(() -> new StorePageNotFoundException(slug));
    }

    @Transactional
    public StorePageResponse createPage(Store store, StorePageRequest request) {
        StorePage page = new StorePage();
        page.setStore(store);
        page.setSlug(uniqueSlug(store, resolveSlug(request), null));
        page.setTitle(request.title().trim());
        page.setContent(request.content().trim());
        page.setPosition(request.position() != null ? request.position() : nextPosition(store));
        return toResponse(storePageRepository.save(page));
    }

    @Transactional
    public StorePageResponse updatePage(Store store, Long id, StorePageRequest request) {
        StorePage page = findByIdAndStoreOrThrow(id, store);
        page.setSlug(uniqueSlug(store, resolveSlug(request), id));
        page.setTitle(request.title().trim());
        page.setContent(request.content().trim());
        if (request.position() != null) {
            page.setPosition(request.position());
        }
        return toResponse(storePageRepository.save(page));
    }

    @Transactional
    public void deletePage(Store store, Long id) {
        storePageRepository.delete(findByIdAndStoreOrThrow(id, store));
    }

    /**
     * Installe les pages livrees. Sans effet sur celles qui existent deja : la
     * methode doit pouvoir etre rappelee sans ecraser le travail du vendeur.
     */
    @Transactional
    public void installDefaults(Store store) {
        int position = 0;
        for (DefaultStorePages.Template template : DefaultStorePages.TEMPLATES) {
            position++;
            if (storePageRepository.existsByStoreAndSlugIgnoreCase(store, template.slug())) {
                continue;
            }

            StorePage page = new StorePage();
            page.setStore(store);
            page.setSlug(template.slug());
            page.setTitle(template.title());
            page.setContent(template.content().trim());
            page.setPosition(position);
            storePageRepository.save(page);
        }
    }

    /**
     * Installe les pages livrees, mais seulement dans une boutique qui n'en a
     * aucune.
     *
     * Une boutique qui possede au moins une page a ete travaillee : y reinjecter
     * les modeles ferait revenir a chaque demarrage une page que le vendeur a
     * justement pris la peine de supprimer.
     */
    @Transactional
    public boolean installDefaultsIfEmpty(Store store) {
        if (!storePageRepository.findAllByStoreOrderByPositionAscIdAsc(store).isEmpty()) {
            return false;
        }
        installDefaults(store);
        return true;
    }

    private StorePage findByIdAndStoreOrThrow(Long id, Store store) {
        return storePageRepository.findByIdAndStore(id, store)
                .orElseThrow(() -> new StorePageNotFoundException(id));
    }

    private int nextPosition(Store store) {
        return storePageRepository.findAllByStoreOrderByPositionAscIdAsc(store).stream()
                .mapToInt(StorePage::getPosition)
                .max()
                .orElse(0) + 1;
    }

    /** Le slug fourni prime ; sinon il derive du titre. */
    private String resolveSlug(StorePageRequest request) {
        String candidate = request.slug() == null || request.slug().isBlank()
                ? request.title()
                : request.slug();
        String slug = slugify(candidate);
        if (slug.isEmpty()) {
            return "page";
        }
        // La colonne accepte 80 caracteres ; on garde de quoi suffixer un doublon.
        return slug.length() > 70 ? slug.substring(0, 70).replaceAll("-$", "") : slug;
    }

    /**
     * Deux pages ne peuvent pas partager une adresse dans la meme boutique. Plutot
     * que de refuser la saisie, on suffixe : le vendeur nomme ses pages comme il
     * l'entend, l'adresse s'arrange.
     */
    private String uniqueSlug(Store store, String base, Long currentId) {
        String candidate = base;
        int suffix = 2;
        while (isTaken(store, candidate, currentId)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private boolean isTaken(Store store, String slug, Long currentId) {
        return currentId == null
                ? storePageRepository.existsByStoreAndSlugIgnoreCase(store, slug)
                : storePageRepository.existsByStoreAndSlugIgnoreCaseAndIdNot(store, slug, currentId);
    }

    private static String slugify(String value) {
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private static StorePageResponse toResponse(StorePage page) {
        return new StorePageResponse(
                page.getId(),
                page.getSlug(),
                page.getTitle(),
                page.getContent(),
                page.getPosition(),
                page.getUpdatedAt()
        );
    }
}
