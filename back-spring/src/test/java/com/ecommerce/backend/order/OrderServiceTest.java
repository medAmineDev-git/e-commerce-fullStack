package com.ecommerce.backend.order;

import com.ecommerce.backend.order.dto.OrderItemRequest;
import com.ecommerce.backend.order.dto.OrderRequest;
import com.ecommerce.backend.order.dto.OrderResponse;
import com.ecommerce.backend.product.Product;
import com.ecommerce.backend.product.ProductNotFoundException;
import com.ecommerce.backend.product.ProductRepository;
import com.ecommerce.backend.store.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Store store;

    @BeforeEach
    void setUp() {
        store = store(1L, "nova");
    }

    @Test
    void placeOrderShouldPersistItemsCalculateTotalAndDecreaseStock() {
        Product product = product(1L, "49.90", 3);
        when(productRepository.findByIdAndStore(1L, store)).thenReturn(Optional.of(product));

        OrderResponse response = orderService.placeOrder(store, request(1L, 2, "0.01"));

        ArgumentCaptor<CustomerOrder> orderCaptor = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(orderRepository).save(orderCaptor.capture());
        CustomerOrder savedOrder = orderCaptor.getValue();

        assertEquals(new BigDecimal("99.80"), response.total());
        assertEquals(OrderService.PENDING_ADMIN_VALIDATION, response.status());
        assertEquals(store, savedOrder.getStore());
        assertEquals(1, savedOrder.getItems().size());
        assertEquals(savedOrder, savedOrder.getItems().getFirst().getOrder());
        assertEquals(1, product.getStockQuantity());
    }

    @Test
    void placeOrderShouldRejectQuantityAboveAvailableStock() {
        when(productRepository.findByIdAndStore(1L, store)).thenReturn(Optional.of(product(1L, "49.90", 1)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.placeOrder(store, request(1L, 2, "99.80"))
        );

        assertEquals("Insufficient stock for product: 1", exception.getMessage());
    }

    /**
     * Le produit d'une autre boutique n'est plus rejete apres coup : il est
     * introuvable, parce que la recherche elle-meme est bornee a la boutique.
     */
    @Test
    void placeOrderShouldNotSeeProductsOfAnotherStore() {
        when(productRepository.findByIdAndStore(1L, store)).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> orderService.placeOrder(store, request(1L, 1, "49.90"))
        );
    }

    @Test
    void getOrderShouldReportAnotherStoreOrderAsNotFound() {
        when(orderRepository.findByOrderNumberAndStore("CMD-ATEL0001", store)).thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrder(store, "CMD-ATEL0001")
        );
    }

    private OrderRequest request(Long productId, int quantity, String total) {
        return new OrderRequest(
                "Alice",
                "0600000000",
                "Paris",
                "10 rue Exemple",
                "",
                "cash_on_delivery",
                List.of(new OrderItemRequest(productId, quantity)),
                new BigDecimal(total)
        );
    }

    private Store store(Long id, String slug) {
        Store store = new Store();
        store.setId(id);
        store.setName(slug);
        store.setSlug(slug);
        return store;
    }

    private Product product(Long id, String price, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setName("Sneaker Urban Pulse");
        product.setCategory("Sneakers");
        product.setDescription("Description");
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(stock);
        product.setStore(store);
        return product;
    }
}
