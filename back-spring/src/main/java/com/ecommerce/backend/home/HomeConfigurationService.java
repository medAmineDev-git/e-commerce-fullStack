package com.ecommerce.backend.home;

import com.ecommerce.backend.home.dto.HomeConfigurationRequest;
import com.ecommerce.backend.home.dto.HomeConfigurationResponse;
import com.ecommerce.backend.product.Product;
import com.ecommerce.backend.product.ProductNotFoundException;
import com.ecommerce.backend.product.ProductRepository;
import com.ecommerce.backend.store.Store;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration de la page d'accueil, propre a chaque boutique.
 */
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

    public HomeConfigurationResponse getConfiguration(Store store) {
        HomeConfiguration configuration = configurationRepository.findByStoreAndConfigKey(store, CONFIG_KEY)
                .or(() -> configurationRepository.findFirstByStore(store))
                .orElseGet(() -> defaultConfiguration(store));
        return toResponse(configuration);
    }

    @Transactional
    public HomeConfigurationResponse saveConfiguration(Store store, HomeConfigurationRequest request) {
        // Le produit mis en avant est cherche dans la boutique elle-meme : une boutique
        // ne peut pas afficher en vitrine le produit d'une autre.
        Product product = productRepository.findByIdAndStore(request.featuredProductId(), store)
                .orElseThrow(() -> new ProductNotFoundException(request.featuredProductId()));

        HomeConfiguration configuration = configurationRepository.findByStoreAndConfigKey(store, CONFIG_KEY)
                .orElseGet(() -> {
                    HomeConfiguration config = new HomeConfiguration();
                    config.setStore(store);
                    config.setConfigKey(CONFIG_KEY);
                    return config;
                });

        configuration.setTitle(request.title().trim());
        configuration.setText(request.text().trim());
        configuration.setFeaturedProductId(product.getId());

        return toResponse(configurationRepository.save(configuration));
    }

    private HomeConfiguration defaultConfiguration(Store store) {
        HomeConfiguration configuration = new HomeConfiguration();
        configuration.setStore(store);
        configuration.setConfigKey(CONFIG_KEY);
        configuration.setTitle("Bienvenue chez " + store.getName());
        configuration.setText(store.getDescription() != null
                ? store.getDescription()
                : "Decouvre notre selection.");
        configuration.setFeaturedProductId(
                productRepository.findAllByStoreOrderByIdDesc(store).stream()
                        .findFirst()
                        .map(Product::getId)
                        .orElse(null)
        );
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
