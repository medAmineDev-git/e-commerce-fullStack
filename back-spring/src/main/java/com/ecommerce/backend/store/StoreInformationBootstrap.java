package com.ecommerce.backend.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Complete les informations de contact des boutiques deja creees.
 *
 * Le pendant de {@link com.ecommerce.backend.page.StorePageBootstrap} pour la
 * section Informations : les boutiques nees avant cette proposition avaient un
 * pied de page vide.
 *
 * Seuls les champs restes vides sont completes. Une boutique qui a renseigne
 * son adresse ou efface volontairement son telephone garde ce qu'elle a mis :
 * un gabarit qui reviendrait a chaque redemarrage serait insupportable.
 */
@Component
public class StoreInformationBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StoreInformationBootstrap.class);

    private final StoreRepository storeRepository;

    public StoreInformationBootstrap(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int completed = 0;
        for (Store store : storeRepository.findAll()) {
            if (DefaultStoreInformation.isUntouched(store)
                    && DefaultStoreInformation.fillBlanks(store)) {
                storeRepository.save(store);
                completed++;
            }
        }

        if (completed > 0) {
            log.info("Informations proposees completees dans {} boutique(s).", completed);
        }
    }
}
