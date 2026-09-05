package com.ecommerce.backend.store;

import com.ecommerce.backend.store.dto.StorePublicResponse;
import com.ecommerce.backend.store.dto.StoreResponse;
import com.ecommerce.backend.store.dto.StoreSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class StoreMapper {

    public StoreResponse toResponse(Store store) {
        if (store == null) {
            return null;
        }
        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getSlug(),
                store.getDescription(),
                store.getLogoUrl(),
                store.getBannerUrl(),
                store.getBannerMobileUrl(),
                store.getPhone(),
                store.getEmail(),
                store.getAddress(),
                store.getDomain(),
                store.isActive(),
                store.getCreatedAt(),
                store.getUpdatedAt(),
                store.getOwner() != null ? store.getOwner().getUsername() : null
        );
    }

    public StorePublicResponse toPublicResponse(Store store) {
        if (store == null) {
            return null;
        }
        return new StorePublicResponse(
                store.getId(),
                store.getName(),
                store.getSlug(),
                store.getDescription(),
                store.getLogoUrl(),
                store.getBannerUrl(),
                store.getBannerMobileUrl(),
                store.getPhone(),
                store.getEmail(),
                store.getAddress(),
                store.getDomain()
        );
    }

    public StoreSummaryResponse toSummaryResponse(Store store) {
        if (store == null) {
            return null;
        }
        return new StoreSummaryResponse(
                store.getId(),
                store.getName(),
                store.getSlug(),
                store.getDomain(),
                store.isActive(),
                store.getOwner() != null ? store.getOwner().getUsername() : null,
                store.getCreatedAt()
        );
    }
}
