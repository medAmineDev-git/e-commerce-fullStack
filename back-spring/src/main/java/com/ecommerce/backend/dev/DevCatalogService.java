package com.ecommerce.backend.dev;

import com.ecommerce.backend.product.Product;
import com.ecommerce.backend.product.ProductRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Profile("dev")
public class DevCatalogService {

    private final ProductRepository productRepository;

    public DevCatalogService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public int reseedCatalog() {
        productRepository.deleteAllInBatch();

        List<Product> products = List.of(
                product("Sneaker Urban Pulse", "Sneakers", "Sneaker polyvalente pour la ville et les trajets quotidiens.", "99.90", 25),
                product("Veste Atelier Marine", "Homme", "Veste legere coupe droite, finition soignee.", "129.00", 12),
                product("Robe Lumiere", "Femme", "Robe fluide avec texture douce pour sorties et occasions.", "89.50", 18),
                product("Sac Echo Mini", "Accessoires", "Sac compact avec compartiments internes pratiques.", "59.00", 30),
                product("Sneaker Horizon Run", "Sneakers", "Modele running amorti pour usage quotidien.", "119.90", 15),
                product("Chemise Studio Blanc", "Homme", "Chemise coton respirant, style epure.", "69.00", 20),
                product("Jupe Riviera", "Femme", "Jupe midi confortable et elegante.", "74.90", 16),
                product("Ceinture Grain Noir", "Accessoires", "Ceinture cuir texture fine, boucle metal.", "39.90", 40)
        );

        return productRepository.saveAll(products).size();
    }

    private Product product(String name, String category, String description, String price, int stockQuantity) {
        Product product = new Product();
        product.setName(name);
        product.setCategory(category);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(stockQuantity);
        return product;
    }
}
