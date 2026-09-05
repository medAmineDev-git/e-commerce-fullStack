package com.ecommerce.backend.page;

import com.ecommerce.backend.page.dto.StorePageRequest;
import com.ecommerce.backend.page.dto.StorePageResponse;
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
 * Pages de contenu vues par le proprietaire de la boutique.
 * La vitrine passe par /api/public/stores/{slug}/pages.
 */
@RestController
@RequestMapping("/api/admin/pages")
public class AdminStorePageController {

    private final StorePageService storePageService;
    private final StoreContext storeContext;

    public AdminStorePageController(StorePageService storePageService, StoreContext storeContext) {
        this.storePageService = storePageService;
        this.storeContext = storeContext;
    }

    @GetMapping
    public List<StorePageResponse> getPages(Authentication authentication) {
        return storePageService.getPages(storeContext.requireOwnedStore(authentication));
    }

    @GetMapping("/{id}")
    public StorePageResponse getPageById(@PathVariable Long id, Authentication authentication) {
        return storePageService.getPageById(storeContext.requireOwnedStore(authentication), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StorePageResponse createPage(
            @Valid @RequestBody StorePageRequest request,
            Authentication authentication
    ) {
        return storePageService.createPage(storeContext.requireOwnedStore(authentication), request);
    }

    @PutMapping("/{id}")
    public StorePageResponse updatePage(
            @PathVariable Long id,
            @Valid @RequestBody StorePageRequest request,
            Authentication authentication
    ) {
        return storePageService.updatePage(storeContext.requireOwnedStore(authentication), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePage(@PathVariable Long id, Authentication authentication) {
        storePageService.deletePage(storeContext.requireOwnedStore(authentication), id);
    }

    /** Reinstalle les pages livrees qui manquent, sans toucher aux autres. */
    @PostMapping("/defaults")
    public List<StorePageResponse> restoreDefaults(Authentication authentication) {
        var store = storeContext.requireOwnedStore(authentication);
        storePageService.installDefaults(store);
        return storePageService.getPages(store);
    }
}
