package com.ecommerce.backend.order;

import com.ecommerce.backend.order.dto.OrderDetailResponse;
import com.ecommerce.backend.order.dto.OrderSummaryResponse;
import com.ecommerce.backend.order.dto.OrderUpdateRequest;
import com.ecommerce.backend.store.StoreContext;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Suivi des commandes par le proprietaire de la boutique.
 * Les clients passent commande via /api/public/stores/{slug}/orders.
 */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;
    private final StoreContext storeContext;

    public AdminOrderController(OrderService orderService, StoreContext storeContext) {
        this.orderService = orderService;
        this.storeContext = storeContext;
    }

    @GetMapping
    public List<OrderSummaryResponse> getOrders(
            @RequestParam(name = "ref", required = false) String publisherRef,
            Authentication authentication
    ) {
        return orderService.getOrders(storeContext.requireOwnedStore(authentication), publisherRef);
    }

    @GetMapping("/{orderNumber}")
    public OrderDetailResponse getOrder(@PathVariable String orderNumber, Authentication authentication) {
        return orderService.getOrder(storeContext.requireOwnedStore(authentication), orderNumber);
    }

    @PutMapping("/{orderNumber}")
    public OrderDetailResponse updateOrder(
            @PathVariable String orderNumber,
            @Valid @RequestBody OrderUpdateRequest request,
            Authentication authentication
    ) {
        return orderService.updateOrder(storeContext.requireOwnedStore(authentication), orderNumber, request);
    }
}
