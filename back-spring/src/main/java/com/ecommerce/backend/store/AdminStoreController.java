package com.ecommerce.backend.store;

import com.ecommerce.backend.store.dto.StoreResponse;
import com.ecommerce.backend.store.dto.StoreUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/store")
public class AdminStoreController {

    private final StoreService storeService;
    private final StoreMapper storeMapper;

    public AdminStoreController(StoreService storeService, StoreMapper storeMapper) {
        this.storeService = storeService;
        this.storeMapper = storeMapper;
    }

    @GetMapping
    public StoreResponse getMyStore(Authentication authentication) {
        Store store = storeService.getStoreForUsername(authentication.getName());
        return storeMapper.toResponse(store);
    }

    @PutMapping
    public StoreResponse updateMyStore(
            Authentication authentication,
            @Valid @RequestBody StoreUpdateRequest request
    ) {
        Store store = storeService.getStoreForUsername(authentication.getName());
        Store updated = storeService.updateStore(store.getId(), request);
        return storeMapper.toResponse(updated);
    }
}
