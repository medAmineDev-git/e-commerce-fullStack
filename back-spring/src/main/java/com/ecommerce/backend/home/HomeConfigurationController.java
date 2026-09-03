package com.ecommerce.backend.home;

import com.ecommerce.backend.home.dto.HomeConfigurationRequest;
import com.ecommerce.backend.home.dto.HomeConfigurationResponse;
import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/home/configuration")
public class HomeConfigurationController {

    private final HomeConfigurationService configurationService;
    private final StoreService storeService;

    public HomeConfigurationController(HomeConfigurationService configurationService, StoreService storeService) {
        this.configurationService = configurationService;
        this.storeService = storeService;
    }

    @GetMapping
    public HomeConfigurationResponse getConfiguration(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            Store store = storeService.getStoreForUsername(authentication.getName());
            return configurationService.getConfiguration(store);
        }
        return configurationService.getConfiguration();
    }

    @PutMapping
    public HomeConfigurationResponse saveConfiguration(
            @Valid @RequestBody HomeConfigurationRequest request,
            Authentication authentication
    ) {
        Store store = resolveStore(authentication);
        return configurationService.saveConfiguration(store, request);
    }

    private Store resolveStore(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            return storeService.getStoreForUsername(authentication.getName());
        }
        return storeService.getStoreEntityById(1L);
    }
}
