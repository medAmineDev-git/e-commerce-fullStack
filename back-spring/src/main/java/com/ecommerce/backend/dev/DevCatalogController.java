package com.ecommerce.backend.dev;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequestMapping("/api/dev/catalog")
public class DevCatalogController {

    private final DevCatalogService devCatalogService;

    public DevCatalogController(DevCatalogService devCatalogService) {
        this.devCatalogService = devCatalogService;
    }

    @PostMapping("/reseed")
    @ResponseStatus(HttpStatus.OK)
    public DevCatalogReseedResponse reseedCatalog() {
        int insertedCount = devCatalogService.reseedCatalog();
        return new DevCatalogReseedResponse("Catalogue dev reinitialise", insertedCount);
    }

    public record DevCatalogReseedResponse(String message, int insertedCount) {
    }
}
