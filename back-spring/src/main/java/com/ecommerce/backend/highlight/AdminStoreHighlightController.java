package com.ecommerce.backend.highlight;

import com.ecommerce.backend.highlight.dto.HighlightSettingsRequest;
import com.ecommerce.backend.highlight.dto.StoreHighlightRequest;
import com.ecommerce.backend.highlight.dto.StoreHighlightResponse;
import com.ecommerce.backend.highlight.dto.StoreHighlightsResponse;
import com.ecommerce.backend.store.StoreContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Bandeau de reassurance vu par le proprietaire de la boutique.
 * La vitrine passe par /api/public/stores/{slug}/highlights.
 */
@RestController
@RequestMapping("/api/admin/highlights")
public class AdminStoreHighlightController {

    private final StoreHighlightService highlightService;
    private final StoreContext storeContext;

    public AdminStoreHighlightController(
            StoreHighlightService highlightService,
            StoreContext storeContext
    ) {
        this.highlightService = highlightService;
        this.storeContext = storeContext;
    }

    @GetMapping
    public StoreHighlightsResponse getHighlights(Authentication authentication) {
        return highlightService.getHighlights(storeContext.requireOwnedStore(authentication));
    }

    /** Les cles d'icones disponibles, pour que le back-office ne les devine pas. */
    @GetMapping("/icons")
    public List<String> getAvailableIcons() {
        return HighlightIcons.KEYS;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreHighlightResponse create(
            @Valid @RequestBody StoreHighlightRequest request,
            Authentication authentication
    ) {
        return highlightService.create(storeContext.requireOwnedStore(authentication), request);
    }

    @PutMapping("/{id}")
    public StoreHighlightResponse update(
            @PathVariable Long id,
            @Valid @RequestBody StoreHighlightRequest request,
            Authentication authentication
    ) {
        return highlightService.update(storeContext.requireOwnedStore(authentication), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        highlightService.delete(storeContext.requireOwnedStore(authentication), id);
    }

    @PutMapping("/settings")
    public StoreHighlightsResponse updateSettings(
            @Valid @RequestBody HighlightSettingsRequest request,
            Authentication authentication
    ) {
        return highlightService.updateSettings(storeContext.requireOwnedStore(authentication), request);
    }
}
