package com.ecommerce.backend.product;

import com.ecommerce.backend.product.dto.ProductRequest;
import com.ecommerce.backend.product.dto.ProductPageResponse;
import com.ecommerce.backend.product.dto.ProductResponse;
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

    @Test
    void getAllProductsShouldReturnMappedResponses() {
        Product first = product(1L, "Sneaker");
        Product second = product(2L, "T-shirt");
        ProductResponse firstResponse = response(1L, "Sneaker");
        ProductResponse secondResponse = response(2L, "T-shirt");

        when(productRepository.findAll()).thenReturn(List.of(first, second));
        when(productMapper.toResponse(first)).thenReturn(firstResponse);
        when(productMapper.toResponse(second)).thenReturn(secondResponse);

        List<ProductResponse> result = productService.getAllProducts();

        assertEquals(2, result.size());
        assertEquals(firstResponse, result.get(0));
        assertEquals(secondResponse, result.get(1));
        verify(productRepository).findAll();
    }

    @Test
    void getProductByIdShouldReturnMappedResponse() {
        Product product = product(1L, "Sneaker");
        ProductResponse response = response(1L, "Sneaker");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.getProductById(1L);

        assertEquals(response, result);
    }

    @Test
    void getProductByIdShouldThrowWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProductById(99L)
        );

        assertEquals("Produit introuvable avec l'id 99", exception.getMessage());
        verify(productMapper, never()).toResponse(org.mockito.ArgumentMatchers.any(Product.class));
    }

    @Test
    void createProductShouldPersistMappedEntity() {
        ProductRequest request = request("Cap");
        Product mapped = product(null, "Cap");
        Product persisted = product(10L, "Cap");
        ProductResponse response = response(10L, "Cap");

        when(productMapper.toEntity(request)).thenReturn(mapped);
        when(productRepository.save(mapped)).thenReturn(persisted);
        when(productMapper.toResponse(persisted)).thenReturn(response);

        ProductResponse result = productService.createProduct(request);

        assertEquals(response, result);
        verify(productMapper).toEntity(request);
        verify(productRepository).save(mapped);
    }

    @Test
    void updateProductShouldUpdateAndReturnMappedResponse() {
        ProductRequest request = request("Updated Name");
        Product existing = product(5L, "Old Name");
        Product updated = product(5L, "Updated Name");
        ProductResponse response = response(5L, "Updated Name");

        when(productRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(updated);
        when(productMapper.toResponse(updated)).thenReturn(response);

        ProductResponse result = productService.updateProduct(5L, request);

        assertEquals(response, result);
        verify(productMapper).updateEntity(existing, request);
        verify(productRepository).save(existing);
    }

    @Test
    void updateProductShouldThrowWhenProductDoesNotExist() {
        ProductRequest request = request("Updated Name");
        when(productRepository.findById(88L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.updateProduct(88L, request));

        verify(productMapper, never()).updateEntity(org.mockito.ArgumentMatchers.any(Product.class), org.mockito.ArgumentMatchers.any(ProductRequest.class));
        verify(productRepository, never()).save(org.mockito.ArgumentMatchers.any(Product.class));
    }

    @Test
    void deleteProductShouldDeleteWhenProductExists() {
        Product existing = product(3L, "Delete Me");
        when(productRepository.findById(3L)).thenReturn(Optional.of(existing));

        productService.deleteProduct(3L);

        verify(productRepository).delete(existing);
    }

    @Test
    void deleteProductShouldThrowWhenProductDoesNotExist() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.deleteProduct(404L));

        verify(productRepository, never()).delete(org.mockito.ArgumentMatchers.any(Product.class));
    }

    @Test
    void searchProductsShouldUseQueryRepositoryMethod() {
        Product first = product(11L, "Sneaker Light");
        Product second = product(12L, "Sneaker Pro");
        Page<Product> page = new PageImpl<>(List.of(first, second), PageRequest.of(0, 2), 2);

        when(productRepository.searchProducts(
                "sneaker",
            "Sneakers",
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "price"))
        )).thenReturn(page);
        when(productMapper.toResponse(first)).thenReturn(response(11L, "Sneaker Light"));
        when(productMapper.toResponse(second)).thenReturn(response(12L, "Sneaker Pro"));

        ProductPageResponse result = productService.searchProducts("sneaker", "Sneakers", 0, 2, "price", "asc");

        assertEquals(2, result.items().size());
        assertEquals(2, result.totalElements());
        assertEquals("price", result.sortBy());
        assertEquals("asc", result.sortDirection());
        assertEquals("sneaker", result.query());
        assertEquals("Sneakers", result.category());
    }

    @Test
    void searchProductsShouldFallbackToDefaultSortAndDirection() {
        Product only = product(2L, "Item");
        Page<Product> page = new PageImpl<>(List.of(only), PageRequest.of(0, 1), 1);

        when(productRepository.searchProducts("", "", PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id"))))
                .thenReturn(page);
        when(productMapper.toResponse(only)).thenReturn(response(2L, "Item"));

        ProductPageResponse result = productService.searchProducts("", "Unknown", 0, 1, "badField", "invalid");

        assertEquals(1, result.items().size());
        assertEquals("id", result.sortBy());
        assertEquals("desc", result.sortDirection());
        assertEquals("", result.category());
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
