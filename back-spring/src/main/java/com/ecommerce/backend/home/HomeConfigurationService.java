package com.ecommerce.backend.home;

import com.ecommerce.backend.home.dto.HomeConfigurationRequest;
import com.ecommerce.backend.home.dto.HomeConfigurationResponse;
import com.ecommerce.backend.store.Store;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Texte d'accueil de la vitrine, propre a chaque boutique.
 *
 * Le produit mis en avant a ete retire : la tete de page ne porte plus qu'une
 * banniere, et la vitrine ne le lisait donc plus. Il restait pourtant exige a
 * l'enregistrement, ce qui empechait une boutique sans aucun produit de
 * sauvegarder son texte.
 */
@Service
@Transactional(readOnly = true)
public class HomeConfigurationService {

    private static final String CONFIG_KEY = "home";

    private final HomeConfigurationRepository configurationRepository;

    public HomeConfigurationService(HomeConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    public HomeConfigurationResponse getConfiguration(Store store) {
        HomeConfiguration configuration = configurationRepository.findByStoreAndConfigKey(store, CONFIG_KEY)
                .or(() -> configurationRepository.findFirstByStore(store))
                .orElseGet(() -> defaultConfiguration(store));
        return toResponse(configuration);
    }

    @Transactional
    public HomeConfigurationResponse saveConfiguration(Store store, HomeConfigurationRequest request) {
        HomeConfiguration configuration = configurationRepository.findByStoreAndConfigKey(store, CONFIG_KEY)
                .orElseGet(() -> {
                    HomeConfiguration config = new HomeConfiguration();
                    config.setStore(store);
                    config.setConfigKey(CONFIG_KEY);
                    return config;
                });

        configuration.setTitle(request.title().trim());
        configuration.setText(request.text().trim());
        // Champ absent : on garde l'etat courant plutot que de masquer un texte
        // que personne n'a demande a cacher.
        if (request.welcomeEnabled() != null) {
            configuration.setWelcomeEnabled(request.welcomeEnabled());
        }

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
        configuration.setWelcomeEnabled(true);
        return configuration;
    }

    private HomeConfigurationResponse toResponse(HomeConfiguration configuration) {
        return new HomeConfigurationResponse(
                configuration.getTitle(),
                configuration.getText(),
                configuration.isWelcomeEnabled()
        );
    }
}
