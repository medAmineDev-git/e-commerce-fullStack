package com.ecommerce.backend.order;

import com.ecommerce.backend.order.dto.OrderDetailResponse;
import com.ecommerce.backend.order.dto.OrderItemRequest;
import com.ecommerce.backend.order.dto.OrderItemResponse;
import com.ecommerce.backend.order.dto.OrderRequest;
import com.ecommerce.backend.order.dto.OrderResponse;
import com.ecommerce.backend.order.dto.OrderSummaryResponse;
import com.ecommerce.backend.order.dto.OrderUpdateRequest;
import com.ecommerce.backend.product.Product;
import com.ecommerce.backend.product.ProductColor;
import com.ecommerce.backend.product.ProductNotFoundException;
import com.ecommerce.backend.product.ProductRepository;
import com.ecommerce.backend.store.Store;
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

/**
 * Toutes les operations sont bornees a une boutique, y compris la lecture des
 * produits commandes : une commande ne peut contenir que des articles de la
 * boutique qui la recoit.
 */
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

    public OrderService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public List<OrderSummaryResponse> getOrders(Store store, String publisherRef) {
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        String safePublisherRef = normalizePublisherRef(publisherRef);
        List<CustomerOrder> orders = safePublisherRef == null
                ? orderRepository.findAllByStore(store, sort)
                : orderRepository.findByStoreAndPublisherRef(store, safePublisherRef, sort);

        return orders.stream().map(this::toSummary).toList();
    }

    public OrderDetailResponse getOrder(Store store, String orderNumber) {
        return toDetail(findByOrderNumberAndStoreOrThrow(orderNumber, store));
    }

    @Transactional
    public OrderDetailResponse updateOrder(Store store, String orderNumber, OrderUpdateRequest request) {
        // L'appartenance est verifiee avant la validation du statut : sinon un statut
        // invalide repondrait 400 sur une commande qui, pour cet appelant, n'existe pas.
        CustomerOrder order = findByOrderNumberAndStoreOrThrow(orderNumber, store);

        if (!ALLOWED_STATUSES.contains(request.status())) {
            throw new IllegalArgumentException("Invalid order status: " + request.status());
        }

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
        BigDecimal subtotal = items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Un seul forfait pour toute la commande, pas un par article.
        BigDecimal deliveryFee = DeliveryFeePolicy.feeFor(subtotal);

        order.setDeliveryFee(deliveryFee);
        order.setTotal(subtotal.add(deliveryFee));
        orderRepository.save(order);

        return new OrderResponse(
                order.getOrderNumber(),
                order.getEstimatedDelivery().format(DELIVERY_FORMATTER),
                order.getDeliveryFee(),
                order.getTotal(),
                order.getStatus(),
                items
        );
    }

    /**
     * Le produit est cherche directement dans la boutique, plutot que charge puis
     * compare : un produit d'une autre boutique est simplement introuvable.
     */
    private OrderItemResponse addItem(CustomerOrder order, OrderItemRequest item, Store store) {
        Product product = productRepository.findByIdAndStore(item.productId(), store)
                .orElseThrow(() -> new ProductNotFoundException(item.productId()));

        if (product.getStockQuantity() < item.quantity()) {
            throw new IllegalArgumentException("Insufficient stock for product: " + product.getId());
        }

        String size = resolveVariant(item.size(), product.getSizes(), "size", product);
        String color = resolveVariant(
                item.color(),
                product.getColors().stream().map(ProductColor::getName).toList(),
                "color",
                product
        );

        product.setStockQuantity(product.getStockQuantity() - item.quantity());

        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(product.getId());
        orderItem.setProductName(product.getName());
        orderItem.setUnitPrice(product.getPrice());
        orderItem.setQuantity(item.quantity());
        orderItem.setSize(size);
        orderItem.setColor(color);
        order.addItem(orderItem);

        return toItemResponse(orderItem);
    }

    /**
     * Une declinaison proposee par le produit doit etre choisie, et parmi
     * celles qu'il propose : sans cela, le vendeur recevrait une commande
     * impossible a preparer. La valeur enregistree est celle du vendeur, pas la
     * saisie du client (« m » devient « M »).
     *
     * Un produit sans declinaison ignore ce qui est envoye : il n'y a rien a choisir.
     */
    private String resolveVariant(String requested, List<String> offered, String kind, Product product) {
        if (offered.isEmpty()) {
            return null;
        }
        if (requested == null || requested.isBlank()) {
            throw new IllegalArgumentException("A " + kind + " must be chosen for product: " + product.getId());
        }

        String wanted = requested.trim();
        return offered.stream()
                .filter(value -> value.equalsIgnoreCase(wanted))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown " + kind + " '" + wanted + "' for product: " + product.getId()));
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSize(),
                item.getColor()
        );
    }

    private CustomerOrder findByOrderNumberAndStoreOrThrow(String orderNumber, Store store) {
        return orderRepository.findByOrderNumberAndStore(orderNumber, store)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
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
                .map(this::toItemResponse)
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
                order.getDeliveryFee(),
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
}
