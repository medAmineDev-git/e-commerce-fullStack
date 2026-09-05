package com.ecommerce.backend.highlight;

import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Dote les boutiques deja creees du bandeau propose.
 *
 * Meme regle que pour les pages : sans effet sur une boutique qui possede deja
 * au moins une ligne, pour qu'une ligne supprimee ne revienne pas au
 * redemarrage suivant.
 */
@Component
public class StoreHighlightBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StoreHighlightBootstrap.class);

    private final StoreRepository storeRepository;
    private final StoreHighlightService highlightService;

    public StoreHighlightBootstrap(
            StoreRepository storeRepository,
            StoreHighlightService highlightService
    ) {
        this.storeRepository = storeRepository;
        this.highlightService = highlightService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int equipped = 0;
        for (Store store : storeRepository.findAll()) {
            if (highlightService.installDefaultsIfEmpty(store)) {
                equipped++;
            }
        }

        if (equipped > 0) {
            log.info("Bandeau propose installe dans {} boutique(s).", equipped);
        }
    }
}
