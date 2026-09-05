package com.ecommerce.backend.highlight;

import com.ecommerce.backend.highlight.dto.HighlightSettingsRequest;
import com.ecommerce.backend.highlight.dto.StoreHighlightRequest;
import com.ecommerce.backend.highlight.dto.StoreHighlightResponse;
import com.ecommerce.backend.highlight.dto.StoreHighlightsResponse;
import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bandeau de reassurance, borne a une boutique.
 *
 * Une ligne desactivee est conservee : le vendeur qui coupe momentanement sa
 * promesse de livraison en 48 h la retrouve intacte, sans avoir a la ressaisir.
 */
@Service
@Transactional(readOnly = true)
public class StoreHighlightService {

    private final StoreHighlightRepository highlightRepository;
    private final StoreRepository storeRepository;

    public StoreHighlightService(
            StoreHighlightRepository highlightRepository,
            StoreRepository storeRepository
    ) {
        this.highlightRepository = highlightRepository;
        this.storeRepository = storeRepository;
    }

    /** Vue du back-office : tout, y compris ce qui est desactive. */
    public StoreHighlightsResponse getHighlights(Store store) {
        return new StoreHighlightsResponse(
                store.isHighlightsTopEnabled(),
                store.isHighlightsBottomEnabled(),
                highlightRepository.findAllByStoreOrderByPositionAscIdAsc(store).stream()
                        .map(StoreHighlightService::toResponse)
                        .toList());
    }

    /** Vue de la vitrine : seulement ce qui est actif. */
    public StoreHighlightsResponse getVisibleHighlights(Store store) {
        return new StoreHighlightsResponse(
                store.isHighlightsTopEnabled(),
                store.isHighlightsBottomEnabled(),
                highlightRepository.findAllByStoreAndEnabledTrueOrderByPositionAscIdAsc(store).stream()
                        .map(StoreHighlightService::toResponse)
                        .toList());
    }

    @Transactional
    public StoreHighlightResponse create(Store store, StoreHighlightRequest request) {
        StoreHighlight highlight = new StoreHighlight();
        highlight.setStore(store);
        apply(highlight, request);
        highlight.setPosition(request.position() != null ? request.position() : nextPosition(store));
        return toResponse(highlightRepository.save(highlight));
    }

    @Transactional
    public StoreHighlightResponse update(Store store, Long id, StoreHighlightRequest request) {
        StoreHighlight highlight = findByIdAndStoreOrThrow(id, store);
        apply(highlight, request);
        if (request.position() != null) {
            highlight.setPosition(request.position());
        }
        return toResponse(highlightRepository.save(highlight));
    }

    @Transactional
    public void delete(Store store, Long id) {
        highlightRepository.delete(findByIdAndStoreOrThrow(id, store));
    }

    @Transactional
    public StoreHighlightsResponse updateSettings(Store store, HighlightSettingsRequest request) {
        store.setHighlightsTopEnabled(request.topEnabled());
        store.setHighlightsBottomEnabled(request.bottomEnabled());
        Store saved = storeRepository.save(store);
        return getHighlights(saved);
    }

    /** Installe le bandeau propose, sans effet si la boutique en a deja un. */
    @Transactional
    public boolean installDefaultsIfEmpty(Store store) {
        if (highlightRepository.countByStore(store) > 0) {
            return false;
        }

        int position = 0;
        for (DefaultStoreHighlights.Template template : DefaultStoreHighlights.TEMPLATES) {
            position++;
            StoreHighlight highlight = new StoreHighlight();
            highlight.setStore(store);
            highlight.setIconKey(template.iconKey());
            highlight.setLabel(template.label());
            highlight.setDetail(template.detail());
            highlight.setEnabled(true);
            highlight.setPosition(position);
            highlightRepository.save(highlight);
        }
        return true;
    }

    private void apply(StoreHighlight highlight, StoreHighlightRequest request) {
        String iconKey = request.iconKey().trim();
        if (!HighlightIcons.isKnown(iconKey)) {
            // Refusee a l'entree plutot que rendue par un carre vide sur la vitrine.
            throw new IllegalArgumentException("Icone inconnue : " + iconKey);
        }

        highlight.setIconKey(iconKey);
        highlight.setLabel(request.label().trim());
        highlight.setDetail(blankToNull(request.detail()));
        highlight.setEnabled(request.enabled() == null || request.enabled());
    }

    private StoreHighlight findByIdAndStoreOrThrow(Long id, Store store) {
        return highlightRepository.findByIdAndStore(id, store)
                .orElseThrow(() -> new StoreHighlightNotFoundException(id));
    }

    private int nextPosition(Store store) {
        return highlightRepository.findAllByStoreOrderByPositionAscIdAsc(store).stream()
                .mapToInt(StoreHighlight::getPosition)
                .max()
                .orElse(0) + 1;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static StoreHighlightResponse toResponse(StoreHighlight highlight) {
        return new StoreHighlightResponse(
                highlight.getId(),
                highlight.getIconKey(),
                highlight.getLabel(),
                highlight.getDetail(),
                highlight.isEnabled(),
                highlight.getPosition());
    }
}
