package com.ecommerce.backend.order;

import com.ecommerce.backend.order.dto.OrderItemRequest;
import com.ecommerce.backend.order.dto.OrderRequest;
import com.ecommerce.backend.order.dto.OrderResponse;
import com.ecommerce.backend.product.Product;
import com.ecommerce.backend.product.ProductColor;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

        // 2 x 49,90 = 99,80, sous le seuil : le forfait s'ajoute. Le total envoye
        // par le client (0,01) n'est pas repris.
        assertEquals(0, new BigDecimal("106.70").compareTo(response.total()));
        assertEquals(0, new BigDecimal("6.90").compareTo(response.deliveryFee()));
        assertEquals(0, response.total().compareTo(savedOrder.getTotal()));
        assertEquals(0, response.deliveryFee().compareTo(savedOrder.getDeliveryFee()));
        assertEquals(OrderService.PENDING_ADMIN_VALIDATION, response.status());
        assertEquals(store, savedOrder.getStore());
        assertEquals(1, savedOrder.getItems().size());
        assertEquals(savedOrder, savedOrder.getItems().getFirst().getOrder());
        assertEquals(1, product.getStockQuantity());
    }

    @Test
    void placeOrderShouldChargeDeliveryOnceForTheWholeOrder() {
        when(productRepository.findByIdAndStore(1L, store)).thenReturn(Optional.of(product(1L, "20.00", 5)));
        when(productRepository.findByIdAndStore(2L, store)).thenReturn(Optional.of(product(2L, "15.00", 5)));
        when(productRepository.findByIdAndStore(3L, store)).thenReturn(Optional.of(product(3L, "10.00", 5)));

        OrderResponse response = orderService.placeOrder(store, request(List.of(
                new OrderItemRequest(1L, 1),
                new OrderItemRequest(2L, 1),
                new OrderItemRequest(3L, 1)
        )));

        assertEquals(0, new BigDecimal("6.90").compareTo(response.deliveryFee()));
        assertEquals(0, new BigDecimal("51.90").compareTo(response.total()));
    }

    /** « Livraison offerte a partir de 100 TND » : 100 pile compte comme atteint. */
    @Test
    void placeOrderShouldOfferDeliveryFromTheThreshold() {
        when(productRepository.findByIdAndStore(1L, store)).thenReturn(Optional.of(product(1L, "50.00", 5)));

        OrderResponse response = orderService.placeOrder(store, request(1L, 2, "0"));

        assertEquals(0, BigDecimal.ZERO.compareTo(response.deliveryFee()));
        assertEquals(0, new BigDecimal("100.00").compareTo(response.total()));
    }

    /** Le vendeur doit savoir quoi preparer : « Body x 1 » ne suffit pas. */
    @Test
    void placeOrderShouldRecordTheChosenSizeAndColorAsTheSellerWroteThem() {
        when(productRepository.findByIdAndStore(1L, store)).thenReturn(Optional.of(variantProduct()));

        OrderResponse response = orderService.placeOrder(store, request(List.of(
                new OrderItemRequest(1L, 1, " 12 MOIS ", "bleu nuit"))));

        ArgumentCaptor<CustomerOrder> orderCaptor = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(orderRepository).save(orderCaptor.capture());
        OrderItem saved = orderCaptor.getValue().getItems().getFirst();

        assertEquals("12 mois", saved.getSize());
        assertEquals("Bleu nuit", saved.getColor());
        assertEquals("12 mois", response.items().getFirst().size());
        assertEquals("Bleu nuit", response.items().getFirst().color());
    }

    @Test
    void placeOrderShouldRejectAMissingSizeWhenTheProductOffersSome() {
        when(productRepository.findByIdAndStore(1L, store)).thenReturn(Optional.of(variantProduct()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.placeOrder(store, request(List.of(
                        new OrderItemRequest(1L, 1, null, "Bleu nuit"))))
        );

        assertEquals("A size must be chosen for product: 1", exception.getMessage());
    }

    @Test
    void placeOrderShouldRejectASizeTheProductDoesNotOffer() {
        Product product = variantProduct();
        when(productRepository.findByIdAndStore(1L, store)).thenReturn(Optional.of(product));

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.placeOrder(store, request(List.of(
                        new OrderItemRequest(1L, 1, "XL", "Bleu nuit"))))
        );
        // Refusee avant de toucher au stock.
        assertEquals(3, product.getStockQuantity());
    }

    /** Un sac n'a pas de taille : ce qui arrive est ignore, rien n'est exige. */
    @Test
    void placeOrderShouldIgnoreVariantsForAProductWithoutAny() {
        when(productRepository.findByIdAndStore(1L, store)).thenReturn(Optional.of(product(1L, "49.90", 3)));

        OrderResponse response = orderService.placeOrder(store, request(List.of(
                new OrderItemRequest(1L, 1, "M", "Sable"))));

        assertNull(response.items().getFirst().size());
        assertNull(response.items().getFirst().color());
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
        return request(List.of(new OrderItemRequest(productId, quantity)), new BigDecimal(total));
    }

    private OrderRequest request(List<OrderItemRequest> items) {
        return request(items, BigDecimal.ZERO);
    }

    private OrderRequest request(List<OrderItemRequest> items, BigDecimal total) {
        return new OrderRequest(
                "Alice",
                "0600000000",
                "Paris",
                "10 rue Exemple",
                "",
                "cash_on_delivery",
                items,
                total
        );
    }

    private Store store(Long id, String slug) {
        Store store = new Store();
        store.setId(id);
        store.setName(slug);
        store.setSlug(slug);
        return store;
    }

    private Product variantProduct() {
        Product product = product(1L, "19.90", 3);
        product.setSizes(new ArrayList<>(List.of("3 mois", "12 mois", "2 ans")));
        product.setColors(new ArrayList<>(List.of(new ProductColor("Bleu nuit", "#1b2a4a"))));
        return product;
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
