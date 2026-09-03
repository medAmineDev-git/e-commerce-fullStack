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

    public Store getStoreForUser(AdminUser user) {
        return storeRepository.findFirstByOwner(user)
                .orElseGet(() -> storeRepository.findById(1L)
                        .orElseThrow(() -> new StoreNotFoundException("Default store not found")));
    }

    public Store getStoreForUsername(String username) {
        return storeRepository.findFirstByOwnerUsernameIgnoreCase(username)
                .orElseGet(() -> storeRepository.findById(1L)
                        .orElseThrow(() -> new StoreNotFoundException("Default store not found")));
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

    @Transactional
    public Store toggleActive(Long storeId) {
        Store store = getStoreEntityById(storeId);
        store.setActive(!store.isActive());
        return storeRepository.save(store);
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
