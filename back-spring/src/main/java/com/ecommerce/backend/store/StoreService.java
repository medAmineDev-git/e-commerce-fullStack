package com.ecommerce.backend.store;

import com.ecommerce.backend.auth.AdminUser;
import com.ecommerce.backend.store.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class StoreService {

    private static final Set<String> RESERVED_SLUGS = Set.of(
            "admin", "api", "auth", "login", "register", "dashboard",
            "pricing", "platform", "static", "assets", "favicon",
            "swagger", "openapi", "products", "categories", "cart",
            "checkout", "shop", "orders", "home", "public"
    );

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final StoreRepository storeRepository;
    private final StoreMapper storeMapper;

    public StoreService(StoreRepository storeRepository, StoreMapper storeMapper) {
        this.storeRepository = storeRepository;
        this.storeMapper = storeMapper;
    }

    public Store getStoreEntityBySlug(String slug) {
        return storeRepository.findBySlugIgnoreCase(slug.trim())
                .filter(Store::isActive)
                .orElseThrow(() -> new StoreNotFoundException(slug));
    }

    public Store getStoreEntityById(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new StoreNotFoundException(id));
    }



    /**
     * Resout la boutique administree par le compte authentifie.
     * Aucun repli sur une boutique par defaut : un compte sans boutique est une
     * situation anormale, pas une invitation a administrer celle des autres.
     */
    public Store getStoreOwnedBy(String username) {
        if (username == null || username.isBlank()) {
            throw new StoreNotFoundException("aucun utilisateur authentifie");
        }
        return storeRepository.findFirstByOwnerUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new StoreNotFoundException("aucune boutique rattachee a " + username));
    }

    /**
     * Boutique designee par le jeton, relue en verifiant qu'elle appartient
     * toujours a l'appelant. Une seule requete, sur la cle primaire.
     */
    public Store getStoreOwnedBy(Long storeId, String username) {
        if (storeId == null || username == null || username.isBlank()) {
            throw new StoreNotFoundException("perimetre de boutique absent du jeton");
        }
        return storeRepository.findByIdAndOwnerUsernameIgnoreCase(storeId, username.trim())
                .orElseThrow(() -> new StoreNotFoundException("boutique " + storeId + " non rattachee a " + username));
    }

    public StorePublicResponse getPublicStoreBySlug(String slug) {
        return storeMapper.toPublicResponse(getStoreEntityBySlug(slug));
    }

    public StorePublicResponse resolveStoreByDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            throw new StoreNotFoundException("Domain is empty");
        }
        String normalizedDomain = domain.trim().toLowerCase(Locale.ROOT)
                .replaceFirst("^https?://", "")
                .replaceFirst("^www\\.", "")
                .split(":")[0];

        Store store = storeRepository.findByDomainIgnoreCase(normalizedDomain)
                .or(() -> storeRepository.findByDomainIgnoreCase("www." + normalizedDomain))
                .filter(Store::isActive)
                .orElseThrow(() -> new StoreNotFoundException("Domain: " + domain));

        return storeMapper.toPublicResponse(store);
    }

    public StoreResponse getStoreResponse(Long id) {
        return storeMapper.toResponse(getStoreEntityById(id));
    }

    public List<StoreSummaryResponse> getAllStores() {
        return storeRepository.findAll().stream()
                .map(storeMapper::toSummaryResponse)
                .toList();
    }

    @Transactional
    public Store createStore(AdminUser owner, StoreCreateRequest request) {
        String baseSlug = (request.slug() != null && !request.slug().isBlank())
                ? slugify(request.slug())
                : slugify(request.name());

        String uniqueSlug = generateUniqueSlug(baseSlug);

        Store store = new Store();
        store.setName(request.name().trim());
        store.setSlug(uniqueSlug);
        store.setDescription(request.description());
        store.setLogoUrl(request.logoUrl());
        store.setBannerUrl(request.bannerUrl());
        store.setPhone(request.phone());
        store.setEmail(request.email());
        store.setAddress(request.address());
        store.setDomain(normalizeDomain(request.domain()));
        store.setActive(true);
        store.setOwner(owner);

        return storeRepository.save(store);
    }

    @Transactional
    public Store updateStore(Long storeId, StoreUpdateRequest request) {
        Store store = getStoreEntityById(storeId);
        store.setName(request.name().trim());
        store.setDescription(request.description());
        store.setLogoUrl(request.logoUrl());
        store.setBannerUrl(request.bannerUrl());
        store.setPhone(request.phone());
        store.setEmail(request.email());
        store.setAddress(request.address());
        store.setDomain(normalizeDomain(request.domain()));

        return storeRepository.save(store);
    }

    /**
     * Un domaine ne peut servir qu'une boutique : sinon la resolution par domaine
     * deviendrait ambigue et servirait la mauvaise vitrine.
     */
    @Transactional
    public Store attachDomain(Long storeId, String domain) {
        Store store = getStoreEntityById(storeId);
        String normalized = normalizeDomain(domain);

        if (normalized != null) {
            storeRepository.findByDomainIgnoreCase(normalized)
                    .filter(existing -> !existing.getId().equals(storeId))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Domain already attached to store: " + existing.getSlug());
                    });
        }

        store.setDomain(normalized);
        return storeRepository.save(store);
    }

    @Transactional
    public Store toggleActive(Long storeId) {
        Store store = getStoreEntityById(storeId);
        store.setActive(!store.isActive());
        return storeRepository.save(store);
    }

    /**
     * Slug propose pour un nom de boutique, et sa disponibilite.
     *
     * Le slug renvoye est celui qui serait effectivement attribue : un nom
     * reserve ou deja pris donne un slug suffixe, et le formulaire l affiche
     * plutot que de laisser l utilisateur decouvrir la substitution apres coup.
     */
    public SlugCheckResponse checkSlugAvailability(String name) {
        String base = slugify(name);
        String attributed = generateUniqueSlug(base);
        return new SlugCheckResponse(attributed, attributed.equals(base));
    }

    public String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "boutique";
        }
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = slug.toLowerCase(Locale.ROOT).replaceAll("-+", "-").replaceAll("^-|-$", "");
        if (slug.isBlank()) {
            return "boutique";
        }
        return slug;
    }

    private String generateUniqueSlug(String baseSlug) {
        String candidate = baseSlug;
        if (RESERVED_SLUGS.contains(candidate.toLowerCase(Locale.ROOT))) {
            candidate = candidate + "-shop";
        }

        int suffix = 2;
        while (storeRepository.existsBySlugIgnoreCase(candidate) || RESERVED_SLUGS.contains(candidate.toLowerCase(Locale.ROOT))) {
            candidate = baseSlug + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private String normalizeDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return null;
        }
        return domain.trim().toLowerCase(Locale.ROOT)
                .replaceFirst("^https?://", "")
                .replaceFirst("^www\\.", "")
                .split(":")[0];
    }
}
