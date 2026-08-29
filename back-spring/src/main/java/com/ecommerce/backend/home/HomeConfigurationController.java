package com.ecommerce.backend.home;

import com.ecommerce.backend.home.dto.HomeConfigurationRequest;
import com.ecommerce.backend.home.dto.HomeConfigurationResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home/configuration")
public class HomeConfigurationController {

    private final HomeConfigurationService configurationService;

    public HomeConfigurationController(HomeConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping
    public HomeConfigurationResponse getConfiguration() {
        return configurationService.getConfiguration();
    }

    @PutMapping
    public HomeConfigurationResponse saveConfiguration(@Valid @RequestBody HomeConfigurationRequest request) {
        return configurationService.saveConfiguration(request);
    }
}
