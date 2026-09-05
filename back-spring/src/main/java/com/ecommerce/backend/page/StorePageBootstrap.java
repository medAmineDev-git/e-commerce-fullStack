package com.ecommerce.backend.page;

import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Dote les boutiques deja creees des pages livrees.
 *
 * La table est arrivee apres elles : sans ce passage, seules les boutiques
 * creees ensuite auraient eu un pied de page rempli. Les textes vivent dans
 * {@link DefaultStorePages} plutot que dans la migration, pour n'exister qu'a
 * un seul endroit.
 *
 * Sans effet sur une boutique qui possede deja au moins une page : une page
 * supprimee volontairement ne doit pas revenir au redemarrage suivant.
 */
@Component
public class StorePageBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StorePageBootstrap.class);

    private final StoreRepository storeRepository;
    private final StorePageService storePageService;

    public StorePageBootstrap(StoreRepository storeRepository, StorePageService storePageService) {
        this.storeRepository = storeRepository;
        this.storePageService = storePageService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int equipped = 0;
        for (Store store : storeRepository.findAll()) {
            if (storePageService.installDefaultsIfEmpty(store)) {
                equipped++;
            }
        }

        if (equipped > 0) {
            log.info("Pages livrees installees dans {} boutique(s).", equipped);
        }
    }
}
