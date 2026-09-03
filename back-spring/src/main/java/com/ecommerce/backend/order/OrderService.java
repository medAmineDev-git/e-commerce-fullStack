package com.ecommerce.backend.order;

import com.ecommerce.backend.order.dto.*;
import com.ecommerce.backend.product.Product;
import com.ecommerce.backend.product.ProductRepository;
import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrderService {

    public static final String PENDING_ADMIN_VALIDATION = "EN_ATTENTE_VALIDATION_ADMIN";
    public static final Set<String> ALLOWED_STATUSES = Set.of(
            PENDING_ADMIN_VALIDATION,
            "ANNULEE",
            "VALIDEE_PAR_LE_CLIENT",
            "LIVREE_ET_PAYEE",
            "RETOURNEE_PAR_LE_CLIENT",
            "LIVRAISON_EN_COURS"
    );
    private static final Set<String> ALLOWED_PUBLISHER_REFERENCES = Set.of("am", "wa");

    private static final DateTimeFormatter DELIVERY_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRANCE);

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;

    public OrderService(ProductRepository productRepository, OrderRepository orderRepository, StoreRepository storeRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
    }

    public List<OrderSummaryResponse> getOrders(Store store, String publisherRef) {
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        String safePublisherRef = normalizePublisherRef(publisherRef);
        List<CustomerOrder> orders = safePublisherRef == null
                ? orderRepository.findAllByStore(store, sort)
                : orderRepository.findByStoreAndPublisherRef(store, safePublisherRef, sort);

        return orders.stream().map(this::toSummary).toList();
    }

    public List<OrderSummaryResponse> getOrders(String publisherRef) {
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        String safePublisherRef = normalizePublisherRef(publisherRef);
        List<CustomerOrder> orders = safePublisherRef == null
                ? orderRepository.findAll(sort)
                : orderRepository.findByPublisherRef(safePublisherRef, sort);

        return orders.stream().map(this::toSummary).toList();
    }

    public List<OrderSummaryResponse> getOrders() {
        return getOrders(null);
    }

    public OrderDetailResponse getOrder(Store store, String orderNumber) {
        CustomerOrder order = orderRepository.findByOrderNumberAndStore(orderNumber, store)
                .orElseThrow(() -> new IllegalArgumentException("Order does not exist: " + orderNumber));
        return toDetail(order);
    }

    public OrderDetailResponse getOrder(String orderNumber) {
        return toDetail(findByOrderNumber(orderNumber));
    }

    @Transactional
    public OrderDetailResponse updateOrder(Store store, String orderNumber, OrderUpdateRequest request) {
        if (!ALLOWED_STATUSES.contains(request.status())) {
            throw new IllegalArgumentException("Invalid order status: " + request.status());
        }

        CustomerOrder order = orderRepository.findByOrderNumberAndStore(orderNumber, store)
                .orElseThrow(() -> new IllegalArgumentException("Order does not exist: " + orderNumber));
        order.setStatus(request.status());
        order.setNote(request.note());
        return toDetail(orderRepository.save(order));
    }

    @Transactional
    public OrderDetailResponse updateOrder(String orderNumber, OrderUpdateRequest request) {
        if (!ALLOWED_STATUSES.contains(request.status())) {
            throw new IllegalArgumentException("Invalid order status: " + request.status());
        }

        CustomerOrder order = findByOrderNumber(orderNumber);
        order.setStatus(request.status());
        order.setNote(request.note());
        return toDetail(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse placeOrder(Store store, OrderRequest request) {
        CustomerOrder order = new CustomerOrder();
        order.setStore(store);
        order.setOrderNumber("CMD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        order.setCustomerName(request.customerName());
        order.setPhone(request.phone());
        order.setCity(request.city());
        order.setAddress(request.address());
        order.setNote(request.note());
        order.setPaymentMethod(request.paymentMethod());
        order.setPublisherRef(normalizePublisherRef(request.publisherRef()));
        order.setStatus(PENDING_ADMIN_VALIDATION);
        order.setEstimatedDelivery(LocalDate.now().plusDays(3));

        List<OrderItemResponse> items = request.items().stream()
                .map(item -> addItem(order, item, store))
                .toList();
        BigDecimal total = items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotal(total);
        orderRepository.save(order);

        return new OrderResponse(
                order.getOrderNumber(),
                order.getEstimatedDelivery().format(DELIVERY_FORMATTER),
                order.getTotal(),
                order.getStatus(),
                items
        );
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        Store defaultStore = getDefaultStore();
        return placeOrder(defaultStore, request);
    }

    private OrderItemResponse addItem(CustomerOrder order, OrderItemRequest item, Store store) {
        Product product = productRepository.findById(item.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product does not exist: " + item.productId()));

        if (store != null && product.getStore() != null && !product.getStore().getId().equals(store.getId())) {
            throw new IllegalArgumentException("Product " + item.productId() + " does not belong to store: " + store.getSlug());
        }

        if (product.getStockQuantity() < item.quantity()) {
            throw new IllegalArgumentException("Insufficient stock for product: " + product.getId());
        }

        product.setStockQuantity(product.getStockQuantity() - item.quantity());

        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(product.getId());
        orderItem.setProductName(product.getName());
        orderItem.setUnitPrice(product.getPrice());
        orderItem.setQuantity(item.quantity());
        order.addItem(orderItem);

        return new OrderItemResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                item.quantity()
        );
    }

    private CustomerOrder findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order does not exist: " + orderNumber));
    }

    private OrderSummaryResponse toSummary(CustomerOrder order) {
        return new OrderSummaryResponse(
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getCity(),
                order.getPaymentMethod(),
                order.getPublisherRef(),
                order.getStatus(),
                order.getEstimatedDelivery().format(DELIVERY_FORMATTER),
                order.getTotal()
        );
    }

    private OrderDetailResponse toDetail(CustomerOrder order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(item.getProductId(), item.getProductName(), item.getUnitPrice(), item.getQuantity()))
                .toList();

        return new OrderDetailResponse(
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getPhone(),
                order.getCity(),
                order.getAddress(),
                order.getNote(),
                order.getPaymentMethod(),
                order.getPublisherRef(),
                order.getStatus(),
                order.getEstimatedDelivery().format(DELIVERY_FORMATTER),
                order.getTotal(),
                items
        );
    }

    private String normalizePublisherRef(String publisherRef) {
        if (publisherRef == null || publisherRef.isBlank()) {
            return null;
        }
        String normalized = publisherRef.trim().toLowerCase(Locale.ROOT);
        return ALLOWED_PUBLISHER_REFERENCES.contains(normalized) ? normalized : null;
    }

    private Store getDefaultStore() {
        return storeRepository.findById(1L)
                .orElseGet(() -> {
                    Store store = new Store();
                    store.setId(1L);
                    store.setName("NOVA");
                    store.setSlug("nova");
                    store.setActive(true);
                    return storeRepository.save(store);
                });
    }
}
