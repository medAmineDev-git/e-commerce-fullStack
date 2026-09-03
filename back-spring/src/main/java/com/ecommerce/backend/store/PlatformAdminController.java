package com.ecommerce.backend.store;

import com.ecommerce.backend.store.dto.StoreSummaryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform/stores")
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
}
