package com.ecommerce.backend.store;

import com.ecommerce.backend.store.dto.StoreDomainRequest;
import com.ecommerce.backend.store.dto.StoreSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Exploitation de la plateforme.
 *
 * L'annotation double la regle de SecurityConfig a dessein : cette console
 * etait accessible a tout ROLE_ADMIN, c'est-a-dire a n'importe quel
 * proprietaire de boutique. Une protection qui ne tient qu'a une ligne de
 * configuration est trop facile a perdre lors d'un remaniement.
 */
@RestController
@RequestMapping("/api/platform/stores")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformAdminController {

    private final StoreService storeService;
    private final StoreMapper storeMapper;

    public PlatformAdminController(StoreService storeService, StoreMapper storeMapper) {
        this.storeService = storeService;
        this.storeMapper = storeMapper;
    }

    @GetMapping
    public List<StoreSummaryResponse> getAllStores() {
        return storeService.getAllStores();
    }

    @PatchMapping("/{id}/toggle-active")
    public StoreSummaryResponse toggleStoreActive(@PathVariable Long id) {
        return storeMapper.toSummaryResponse(storeService.toggleActive(id));
    }

    /**
     * Rattache un domaine propre a une boutique. Le domaine devient aussitot une
     * origine autorisee, sans redeploiement.
     */
    @PutMapping("/{id}/domain")
    public StoreSummaryResponse attachDomain(@PathVariable Long id, @Valid @RequestBody StoreDomainRequest request) {
        return storeMapper.toSummaryResponse(storeService.attachDomain(id, request.domain()));
    }
}
