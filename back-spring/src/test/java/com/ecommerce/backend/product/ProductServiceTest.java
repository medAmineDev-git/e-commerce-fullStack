package com.ecommerce.backend.product;

import com.ecommerce.backend.product.dto.ProductRequest;
import com.ecommerce.backend.product.dto.ProductPageResponse;
import com.ecommerce.backend.product.dto.ProductResponse;
import com.ecommerce.backend.store.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private Store store;

    @BeforeEach
    void setUp() {
        store = new Store();
        store.setId(1L);
        store.setName("NOVA");
        store.setSlug("nova");
    }

    @Test
    void getAllProductsShouldReturnMappedResponses() {
        Product first = product(1L, "Sneaker");
        Product second = product(2L, "T-shirt");
        ProductResponse firstResponse = response(1L, "Sneaker");
        ProductResponse secondResponse = response(2L, "T-shirt");

        when(productRepository.findAllByStoreOrderByIdDesc(store)).thenReturn(List.of(first, second));
        when(productMapper.toResponse(first)).thenReturn(firstResponse);
        when(productMapper.toResponse(second)).thenReturn(secondResponse);

        List<ProductResponse> result = productService.getAllProducts(store);

        assertEquals(2, result.size());
        assertEquals(firstResponse, result.get(0));
        assertEquals(secondResponse, result.get(1));
        verify(productRepository).findAllByStoreOrderByIdDesc(store);
    }

    @Test
    void getProductByIdShouldReturnMappedResponse() {
        Product product = product(1L, "Sneaker");
        ProductResponse response = response(1L, "Sneaker");

        when(productRepository.findByIdAndStore(1L, store)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.getProductById(store, 1L);

        assertEquals(response, result);
    }

    @Test
    void getProductByIdShouldThrowWhenNotFoundInThisStore() {
        when(productRepository.findByIdAndStore(99L, store)).thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProductById(store, 99L)
        );

        assertEquals("Produit introuvable avec l'id 99", exception.getMessage());
        verify(productMapper, never()).toResponse(org.mockito.ArgumentMatchers.any(Product.class));
    }

    @Test
    void createProductShouldAttachTheProductToTheStore() {
        ProductRequest request = request("Cap");
        Product mapped = product(null, "Cap");
        Product persisted = product(10L, "Cap");
        ProductResponse response = response(10L, "Cap");

        when(productMapper.toEntity(request)).thenReturn(mapped);
        when(productRepository.save(mapped)).thenReturn(persisted);
        when(productMapper.toResponse(persisted)).thenReturn(response);

        ProductResponse result = productService.createProduct(store, request);

        assertEquals(response, result);
        assertEquals(store, mapped.getStore());
        verify(productMapper).toEntity(request);
        verify(productRepository).save(mapped);
    }

    @Test
    void updateProductShouldUpdateAndReturnMappedResponse() {
        ProductRequest request = request("Updated Name");
        Product existing = product(5L, "Old Name");
        Product updated = product(5L, "Updated Name");
        ProductResponse response = response(5L, "Updated Name");

        when(productRepository.findByIdAndStore(5L, store)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(updated);
        when(productMapper.toResponse(updated)).thenReturn(response);

        ProductResponse result = productService.updateProduct(store, 5L, request);

        assertEquals(response, result);
        verify(productMapper).updateEntity(existing, request);
        verify(productRepository).save(existing);
    }

    @Test
    void updateProductShouldThrowWhenProductBelongsToAnotherStore() {
        ProductRequest request = request("Updated Name");
        when(productRepository.findByIdAndStore(88L, store)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.updateProduct(store, 88L, request));

        verify(productMapper, never()).updateEntity(org.mockito.ArgumentMatchers.any(Product.class), org.mockito.ArgumentMatchers.any(ProductRequest.class));
        verify(productRepository, never()).save(org.mockito.ArgumentMatchers.any(Product.class));
    }

    @Test
    void deleteProductShouldDeleteWhenProductBelongsToTheStore() {
        Product existing = product(3L, "Delete Me");
        when(productRepository.findByIdAndStore(3L, store)).thenReturn(Optional.of(existing));

        productService.deleteProduct(store, 3L);

        verify(productRepository).delete(existing);
    }

    @Test
    void deleteProductShouldThrowWhenProductBelongsToAnotherStore() {
        when(productRepository.findByIdAndStore(404L, store)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.deleteProduct(store, 404L));

        verify(productRepository, never()).delete(org.mockito.ArgumentMatchers.any(Product.class));
    }

    @Test
    void searchProductsShouldUseTheStoreScopedRepositoryMethod() {
        Product first = product(11L, "Sneaker Light");
        Product second = product(12L, "Sneaker Pro");
        Page<Product> page = new PageImpl<>(List.of(first, second), PageRequest.of(0, 2), 2);

        when(productRepository.search(
                store, "Sneakers", null, null, "M", "Noir",
                new BigDecimal("20"), new BigDecimal("200"),
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "price"))
        )).thenReturn(page);
        when(productMapper.toResponse(first)).thenReturn(response(11L, "Sneaker Light"));
        when(productMapper.toResponse(second)).thenReturn(response(12L, "Sneaker Pro"));

        ProductPageResponse result = productService.searchProducts(
                store, "Sneakers", "", "", "M", "Noir",
                new BigDecimal("20"), new BigDecimal("200"), 0, 2, "price", "asc");

        assertEquals(2, result.items().size());
        assertEquals(2, result.totalElements());
        assertEquals("price", result.sortBy());
        assertEquals("asc", result.sortDirection());
        assertEquals("Sneakers", result.category());
    }

    /** Un critere vide vaut absence de critere, et non une valeur a rechercher. */
    @Test
    void searchProductsShouldTreatBlankCriteriaAsAbsent() {
        Product only = product(2L, "Item");
        Page<Product> page = new PageImpl<>(List.of(only), PageRequest.of(0, 1), 1);

        when(productRepository.search(
                store, null, null, null, null, null, null, null,
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id"))))
                .thenReturn(page);
        when(productMapper.toResponse(only)).thenReturn(response(2L, "Item"));

        ProductPageResponse result = productService.searchProducts(
                store, "", "  ", "", "", "", null, null, 0, 1, "badField", "invalid");

        assertEquals(1, result.items().size());
        assertEquals("id", result.sortBy());
        assertEquals("desc", result.sortDirection());
    }

    private Product product(Long id, String name) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setCategory("Sneakers");
        product.setDescription("Description");
        product.setPrice(new BigDecimal("29.90"));
        product.setStockQuantity(12);
        return product;
    }

    private ProductRequest request(String name) {
        return new ProductRequest(
                name,
                "Sneakers",
                "Description",
                new BigDecimal("29.90"),
                12
        );
    }

    private ProductResponse response(Long id, String name) {
        return new ProductResponse(
                id,
                name,
                "Sneakers",
                "Description",
                new BigDecimal("29.90"),
                12
        );
    }
}
