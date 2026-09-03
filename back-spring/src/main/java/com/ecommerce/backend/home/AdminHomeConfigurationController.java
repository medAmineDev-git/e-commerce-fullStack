package com.ecommerce.backend.home;

import com.ecommerce.backend.home.dto.HomeConfigurationRequest;
import com.ecommerce.backend.home.dto.HomeConfigurationResponse;
import com.ecommerce.backend.store.StoreContext;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Reglage de la page d'accueil par le proprietaire de la boutique.
 * La vitrine lit la sienne via /api/public/stores/{slug}/home.
 */
@RestController
@RequestMapping("/api/admin/home/configuration")
public class AdminHomeConfigurationController {

    private final HomeConfigurationService configurationService;
    private final StoreContext storeContext;

    public AdminHomeConfigurationController(
            HomeConfigurationService configurationService,
            StoreContext storeContext
    ) {
        this.configurationService = configurationService;
        this.storeContext = storeContext;
    }

    @GetMapping
    public HomeConfigurationResponse getConfiguration(Authentication authentication) {
        return configurationService.getConfiguration(storeContext.requireOwnedStore(authentication));
    }

    @PutMapping
    public HomeConfigurationResponse saveConfiguration(
            @Valid @RequestBody HomeConfigurationRequest request,
            Authentication authentication
    ) {
        return configurationService.saveConfiguration(storeContext.requireOwnedStore(authentication), request);
    }
}
