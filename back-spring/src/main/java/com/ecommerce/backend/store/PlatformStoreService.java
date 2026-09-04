package com.ecommerce.backend.store;

import com.ecommerce.backend.auth.AdminUser;
import com.ecommerce.backend.auth.AdminUserRepository;
import com.ecommerce.backend.category.CategoryRepository;
import com.ecommerce.backend.order.CustomerOrder;
import com.ecommerce.backend.order.OrderRepository;
import com.ecommerce.backend.product.ProductImageStorageService;
import com.ecommerce.backend.product.ProductRepository;
import com.ecommerce.backend.store.dto.StoreDetailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Operations reservees a l'exploitant de la plateforme.
 *
 * Elles sont volontairement separees de {@link StoreService}, qui sert le
 * perimetre d'un proprietaire : melanger les deux ferait cohabiter dans une
 * meme classe des methodes bornees a une boutique et des methodes qui les
 * traversent toutes.
 */
@Service
@Transactional(readOnly = true)
public class PlatformStoreService {

    private static final Logger log = LoggerFactory.getLogger(PlatformStoreService.class);

    private final StoreRepository storeRepository;
    private final AdminUserRepository adminUserRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final ProductImageStorageService imageStorageService;

    public PlatformStoreService(
            StoreRepository storeRepository,
            AdminUserRepository adminUserRepository,
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            OrderRepository orderRepository,
            ProductImageStorageService imageStorageService
    ) {
        this.storeRepository = storeRepository;
        this.adminUserRepository = adminUserRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.orderRepository = orderRepository;
        this.imageStorageService = imageStorageService;
    }

    public StoreDetailResponse getDetail(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        AdminUser owner = store.getOwner();

        return new StoreDetailResponse(
                store.getId(),
                store.getName(),
                store.getSlug(),
                store.getDescription(),
                store.getDomain(),
                store.isActive(),
                store.getCreatedAt(),
                store.getUpdatedAt(),
                owner != null ? owner.getUsername() : null,
                owner != null ? owner.getEmail() : null,
                owner != null ? owner.getRole() : null,
                store.getPhone(),
                store.getEmail(),
                store.getAddress(),
                productRepository.countByStore(store),
                categoryRepository.countByStore(store),
                orderRepository.countByStore(store),
                imageStorageService.usedBytes(store)
        );
    }

    /**
     * Supprime definitivement une boutique, tout son contenu et son compte
     * proprietaire.
     *
     * L'ordre compte, et il est impose par la base :
     *
     *  - les commandes bloquent la suppression de la boutique (ON DELETE
     *    RESTRICT). Elles sont donc retirees d'abord, ce qui emporte leurs
     *    lignes par la cascade JPA.
     *  - produits, categories et configuration d'accueil partent avec la
     *    boutique, en cascade au niveau de la base.
     *  - le compte proprietaire bloque tant que sa boutique existe
     *    (ON DELETE RESTRICT depuis V110), il vient donc en dernier.
     *
     * Les visuels sont effaces du disque a la fin : une transaction ne sait
     * pas annuler une suppression de fichier, mieux vaut qu'elle ait deja
     * reussi en base.
     */
    @Transactional
    public void deleteStore(Long storeId, String requestedBy) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));

        AdminUser owner = store.getOwner();

        // Un exploitant qui tient aussi une boutique ne doit pas pouvoir
        // supprimer la sienne par cet ecran : il se retirerait son propre compte.
        if (owner != null && owner.getUsername().equalsIgnoreCase(requestedBy)) {
            throw new IllegalArgumentException(
                    "Vous ne pouvez pas supprimer votre propre boutique depuis la console.");
        }

        List<CustomerOrder> orders = orderRepository.findAllByStore(store);
        long orderCount = orders.size();
        long productCount = productRepository.countByStore(store);

        orderRepository.deleteAll(orders);
        orderRepository.flush();

        storeRepository.delete(store);
        storeRepository.flush();

        if (owner != null) {
            adminUserRepository.delete(owner);
        }

        imageStorageService.deleteAll(store);

        log.warn("Boutique '{}' supprimee par '{}' : {} produit(s), {} commande(s), compte '{}'.",
                store.getSlug(), requestedBy, productCount, orderCount,
                owner != null ? owner.getUsername() : "sans proprietaire");
    }
}
