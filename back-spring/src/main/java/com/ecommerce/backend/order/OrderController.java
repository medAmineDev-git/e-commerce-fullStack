package com.ecommerce.backend.order;

import com.ecommerce.backend.order.dto.OrderDetailResponse;
import com.ecommerce.backend.order.dto.OrderRequest;
import com.ecommerce.backend.order.dto.OrderResponse;
import com.ecommerce.backend.order.dto.OrderSummaryResponse;
import com.ecommerce.backend.order.dto.OrderUpdateRequest;
import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final StoreService storeService;

    public OrderController(OrderService orderService, StoreService storeService) {
        this.orderService = orderService;
        this.storeService = storeService;
    }

    @GetMapping
    public List<OrderSummaryResponse> getOrders(
            @RequestParam(name = "ref", required = false) String publisherRef,
            Authentication authentication
    ) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            Store store = storeService.getStoreForUsername(authentication.getName());
            return orderService.getOrders(store, publisherRef);
        }
        return orderService.getOrders(publisherRef);
    }

    @GetMapping("/{orderNumber}")
    public OrderDetailResponse getOrder(@PathVariable String orderNumber, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            Store store = storeService.getStoreForUsername(authentication.getName());
            return orderService.getOrder(store, orderNumber);
        }
        return orderService.getOrder(orderNumber);
    }

    @PutMapping("/{orderNumber}")
    public OrderDetailResponse updateOrder(
            @PathVariable String orderNumber,
            @Valid @RequestBody OrderUpdateRequest request,
            Authentication authentication
    ) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            Store store = storeService.getStoreForUsername(authentication.getName());
            return orderService.updateOrder(store, orderNumber, request);
        }
        return orderService.updateOrder(orderNumber, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@Valid @RequestBody OrderRequest request) {
        return orderService.placeOrder(request);
    }
}
