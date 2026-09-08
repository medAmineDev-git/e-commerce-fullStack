package com.ecommerce.backend.config;

import com.ecommerce.backend.store.StoreRepository;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Domaines rattaches a une boutique active, tenus en cache.
 *
 * Deux endroits ont besoin de cette liste : les origines autorisees en CORS, et
 * le choix du fichier HTML servi a la racine. Chacun avec son propre cache
 * aurait double les requetes et laisse les deux se desynchroniser le temps de
 * leurs expirations respectives.
 */
@Component
public class StoreDomainRegistry {

    private static final long CACHE_TTL_MILLIS = 60_000L;

    private final StoreRepository storeRepository;

    private final ConcurrentHashMap<String, Boolean> knownDomains = new ConcurrentHashMap<>();
    private volatile long lastRefresh;

    public StoreDomainRegistry(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    /** Le nom d'hote designe-t-il une boutique ? La forme www. est acceptee. */
    public boolean isStoreDomain(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }

        refreshIfStale();
        return knownDomains.containsKey(host.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Les domaines changent rarement : une minute de cache evite une requete par
     * appel, sans imposer un redemarrage apres un rattachement.
     */
    private void refreshIfStale() {
        long now = System.currentTimeMillis();
        if (now - lastRefresh < CACHE_TTL_MILLIS && lastRefresh != 0L) {
            return;
        }

        synchronized (this) {
            if (now - lastRefresh < CACHE_TTL_MILLIS && lastRefresh != 0L) {
                return;
            }

            Set<String> refreshed = ConcurrentHashMap.newKeySet();
            storeRepository.findAll().stream()
                    .filter(store -> store.isActive()
                            && store.getDomain() != null
                            && !store.getDomain().isBlank())
                    .forEach(store -> {
                        String domain = store.getDomain().trim().toLowerCase(Locale.ROOT);
                        refreshed.add(domain);
                        refreshed.add("www." + domain);
                    });

            knownDomains.keySet().retainAll(refreshed);
            refreshed.forEach(domain -> knownDomains.put(domain, Boolean.TRUE));
            lastRefresh = now;
        }
    }
}
