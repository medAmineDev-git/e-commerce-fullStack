package com.ecommerce.backend.store;

import com.ecommerce.backend.product.ProductImageStorageService;
import com.ecommerce.backend.store.dto.StoreResponse;
import com.ecommerce.backend.store.dto.StoreUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/store")
public class AdminStoreController {

    private final StoreService storeService;
    private final StoreMapper storeMapper;
    private final StoreContext storeContext;
    private final ProductImageStorageService imageStorageService;

    public AdminStoreController(
            StoreService storeService,
            StoreMapper storeMapper,
            StoreContext storeContext,
            ProductImageStorageService imageStorageService
    ) {
        this.storeContext = storeContext;
        this.storeService = storeService;
        this.storeMapper = storeMapper;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping
    public StoreResponse getMyStore(Authentication authentication) {
        Store store = storeContext.requireOwnedStore(authentication);
        return storeMapper.toResponse(store);
    }

    @PutMapping
    public StoreResponse updateMyStore(
            Authentication authentication,
            @Valid @RequestBody StoreUpdateRequest request
    ) {
        Store store = storeContext.requireOwnedStore(authentication);
        Store updated = storeService.updateStore(store.getId(), request);
        return storeMapper.toResponse(updated);
    }

    /**
     * Depot du logo ou de la banniere.
     *
     * Le meme stockage que les visuels produits : il est deja partitionne par
     * boutique et donne des noms non devinables. Le fichier est enregistre puis
     * son URL renvoyee, a charge du formulaire de l'enregistrer avec le reste
     * des reglages.
     */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StoreImageResponse uploadImage(
            @RequestPart("file") @NotNull MultipartFile file,
            Authentication authentication
    ) {
        Store store = storeContext.requireOwnedStore(authentication);
        return new StoreImageResponse(imageStorageService.store(store, file));
    }

    @DeleteMapping("/images")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@RequestParam("url") String url, Authentication authentication) {
        Store store = storeContext.requireOwnedStore(authentication);
        imageStorageService.delete(store, url);
    }

    public record StoreImageResponse(String url) {
    }
}
