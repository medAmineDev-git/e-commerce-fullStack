package com.ecommerce.backend.order;

import com.ecommerce.backend.order.dto.OrderRequest;
import com.ecommerce.backend.order.dto.OrderResponse;
import com.ecommerce.backend.order.dto.OrderSummaryResponse;
import com.ecommerce.backend.order.dto.OrderDetailResponse;
import com.ecommerce.backend.order.dto.OrderUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderSummaryResponse> getOrders(
            @RequestParam(name = "ref", required = false) String publisherRef
    ) {
        return orderService.getOrders(publisherRef);
    }

    @GetMapping("/{orderNumber}")
    public OrderDetailResponse getOrder(@PathVariable String orderNumber) {
        return orderService.getOrder(orderNumber);
    }

    @PutMapping("/{orderNumber}")
    public OrderDetailResponse updateOrder(
            @PathVariable String orderNumber,
            @Valid @RequestBody OrderUpdateRequest request
    ) {
        return orderService.updateOrder(orderNumber, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@Valid @RequestBody OrderRequest request) {
        return orderService.placeOrder(request);
    }
}
