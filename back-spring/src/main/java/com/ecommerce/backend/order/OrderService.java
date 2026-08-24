package com.ecommerce.backend.order;

import com.ecommerce.backend.order.dto.OrderItemRequest;
import com.ecommerce.backend.order.dto.OrderItemResponse;
import com.ecommerce.backend.order.dto.OrderRequest;
import com.ecommerce.backend.order.dto.OrderResponse;
import com.ecommerce.backend.product.Product;
import com.ecommerce.backend.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final DateTimeFormatter DELIVERY_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRANCE);

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public OrderService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        CustomerOrder order = new CustomerOrder();
        order.setOrderNumber("CMD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        order.setCustomerName(request.customerName());
        order.setPhone(request.phone());
        order.setCity(request.city());
        order.setAddress(request.address());
        order.setNote(request.note());
        order.setPaymentMethod(request.paymentMethod());
        order.setStatus("confirmed");
        order.setEstimatedDelivery(LocalDate.now().plusDays(3));

        List<OrderItemResponse> items = request.items().stream()
                .map(item -> addItem(order, item))
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

    private OrderItemResponse addItem(CustomerOrder order, OrderItemRequest item) {
        Product product = productRepository.findById(item.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product does not exist: " + item.productId()));
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
}
