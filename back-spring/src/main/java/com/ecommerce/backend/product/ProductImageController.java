package com.ecommerce.backend.product;

import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreContext;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/products/images")
public class ProductImageController {

    private final ProductImageStorageService imageStorageService;
    private final StoreContext storeContext;

    public ProductImageController(ProductImageStorageService imageStorageService, StoreContext storeContext) {
        this.imageStorageService = imageStorageService;
        this.storeContext = storeContext;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProductImageResponse uploadImage(
            @RequestPart("file") @NotNull MultipartFile file,
            Authentication authentication
    ) {
        Store store = storeContext.requireOwnedStore(authentication);
        return new ProductImageResponse(imageStorageService.store(store, file));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@RequestParam("url") String url, Authentication authentication) {
        Store store = storeContext.requireOwnedStore(authentication);
        imageStorageService.delete(store, url);
    }

    @GetMapping("/usage")
    public StorageUsageResponse usage(Authentication authentication) {
        Store store = storeContext.requireOwnedStore(authentication);
        return new StorageUsageResponse(
                imageStorageService.usedBytes(store),
                imageStorageService.quotaBytes()
        );
    }

    public record ProductImageResponse(String url) {
    }

    public record StorageUsageResponse(long usedBytes, long quotaBytes) {
    }
}
