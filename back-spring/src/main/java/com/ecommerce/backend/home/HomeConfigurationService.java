package com.ecommerce.backend.home;

import com.ecommerce.backend.home.dto.HomeConfigurationRequest;
import com.ecommerce.backend.home.dto.HomeConfigurationResponse;
import com.ecommerce.backend.product.Product;
import com.ecommerce.backend.product.ProductRepository;
import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HomeConfigurationService {

    private static final String CONFIG_KEY = "home";

    private final HomeConfigurationRepository configurationRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    public HomeConfigurationService(
            HomeConfigurationRepository configurationRepository,
            ProductRepository productRepository,
            StoreRepository storeRepository
    ) {
        this.configurationRepository = configurationRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
    }

    public HomeConfigurationResponse getConfiguration(Store store) {
        HomeConfiguration configuration = configurationRepository.findByStoreAndConfigKey(store, CONFIG_KEY)
                .or(() -> configurationRepository.findFirstByStore(store))
                .orElseGet(() -> defaultConfiguration(store));
        return toResponse(configuration);
    }

    public HomeConfigurationResponse getConfiguration() {
        Store defaultStore = getDefaultStore();
        return getConfiguration(defaultStore);
    }

    @Transactional
    public HomeConfigurationResponse saveConfiguration(Store store, HomeConfigurationRequest request) {
        Product product = productRepository.findById(request.featuredProductId())
                .orElseThrow(() -> new IllegalArgumentException("Featured product does not exist: " + request.featuredProductId()));

        if (store != null && product.getStore() != null && !product.getStore().getId().equals(store.getId())) {
            throw new IllegalArgumentException("Featured product does not belong to this store");
        }

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

    @Transactional
    public HomeConfigurationResponse saveConfiguration(HomeConfigurationRequest request) {
        Store defaultStore = getDefaultStore();
        return saveConfiguration(defaultStore, request);
    }

    private HomeConfiguration defaultConfiguration(Store store) {
        HomeConfiguration configuration = new HomeConfiguration();
        configuration.setStore(store);
        configuration.setConfigKey(CONFIG_KEY);
        configuration.setTitle(store != null ? "Bienvenue chez " + store.getName() : "Style urbain, livraison rapide, paiement a la livraison.");
        configuration.setText(store != null && store.getDescription() != null
                ? store.getDescription()
                : "Decouvre une selection orientee streetwear premium avec une experience mobile ultra simple.");

        Long featuredId = store != null
                ? productRepository.findAllByStoreOrderByIdDesc(store).stream().findFirst().map(Product::getId).orElse(null)
                : productRepository.findAll().stream().findFirst().map(Product::getId).orElse(null);

        configuration.setFeaturedProductId(featuredId);
        return configuration;
    }

    private HomeConfigurationResponse toResponse(HomeConfiguration configuration) {
        return new HomeConfigurationResponse(
                configuration.getTitle(),
                configuration.getText(),
                configuration.getFeaturedProductId()
        );
    }

    private Store getDefaultStore() {
        return storeRepository.findById(1L)
                .orElseGet(() -> {
                    Store store = new Store();
                    store.setId(1L);
                    store.setName("NOVA");
                    store.setSlug("nova");
                    store.setActive(true);
                    return storeRepository.save(store);
                });
    }
}
