package com.ecommerce.backend.home;

import com.ecommerce.backend.home.dto.HomeConfigurationRequest;
import com.ecommerce.backend.home.dto.HomeConfigurationResponse;
import com.ecommerce.backend.product.Product;
import com.ecommerce.backend.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HomeConfigurationService {

    private static final String CONFIG_KEY = "home";

    private final HomeConfigurationRepository configurationRepository;
    private final ProductRepository productRepository;

    public HomeConfigurationService(
            HomeConfigurationRepository configurationRepository,
            ProductRepository productRepository
    ) {
        this.configurationRepository = configurationRepository;
        this.productRepository = productRepository;
    }

    public HomeConfigurationResponse getConfiguration() {
        HomeConfiguration configuration = configurationRepository.findByConfigKey(CONFIG_KEY)
                .orElseGet(this::defaultConfiguration);
        return toResponse(configuration);
    }

    @Transactional
    public HomeConfigurationResponse saveConfiguration(HomeConfigurationRequest request) {
        Product product = productRepository.findById(request.featuredProductId())
                .orElseThrow(() -> new IllegalArgumentException("Featured product does not exist: " + request.featuredProductId()));

        HomeConfiguration configuration = configurationRepository.findByConfigKey(CONFIG_KEY)
                .orElseGet(HomeConfiguration::new);
        configuration.setConfigKey(CONFIG_KEY);
        configuration.setTitle(request.title().trim());
        configuration.setText(request.text().trim());
        configuration.setFeaturedProductId(product.getId());

        return toResponse(configurationRepository.save(configuration));
    }

    private HomeConfiguration defaultConfiguration() {
        HomeConfiguration configuration = new HomeConfiguration();
        configuration.setConfigKey(CONFIG_KEY);
        configuration.setTitle("Style urbain, livraison rapide, paiement a la livraison.");
        configuration.setText("Decouvre une selection orientee streetwear premium avec une experience mobile ultra simple.");
        configuration.setFeaturedProductId(productRepository.findAll().stream().findFirst().map(Product::getId).orElse(null));
        return configuration;
    }

    private HomeConfigurationResponse toResponse(HomeConfiguration configuration) {
        return new HomeConfigurationResponse(
                configuration.getTitle(),
                configuration.getText(),
                configuration.getFeaturedProductId()
        );
    }
}
