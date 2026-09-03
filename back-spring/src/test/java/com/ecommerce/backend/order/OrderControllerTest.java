package com.ecommerce.backend.order;

import com.ecommerce.backend.order.dto.OrderItemRequest;
import com.ecommerce.backend.order.dto.OrderItemResponse;
import com.ecommerce.backend.order.dto.OrderRequest;
import com.ecommerce.backend.order.dto.OrderResponse;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.ecommerce.backend.common.exception.GlobalExceptionHandler.class)
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private com.ecommerce.backend.store.StoreService storeService;

    @Test
    void placeOrderShouldReturn200WhenPayloadIsValid() throws Exception {
        OrderRequest request = request();

        OrderResponse response = new OrderResponse(
                "CMD-123456",
                "24 aout 2026",
                new BigDecimal("199.80"),
                "confirmed",
                List.of(new OrderItemResponse(1L, "Sneaker Urban Pulse", new BigDecimal("99.90"), 2))
        );

        when(orderService.placeOrder(request)).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("CMD-123456"))
                .andExpect(jsonPath("$.status").value("confirmed"))
                .andExpect(jsonPath("$.items[0].productId").value(1));
    }

    @Test
    void placeOrderShouldReturn400WhenPayloadIsInvalid() throws Exception {
        OrderRequest invalid = new OrderRequest(
                "",
                "",
                "",
                "",
                "",
                "",
                List.of(),
                new BigDecimal("0")
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.customerName").exists())
                .andExpect(jsonPath("$.validationErrors.phone").exists())
                .andExpect(jsonPath("$.validationErrors.city").exists())
                .andExpect(jsonPath("$.validationErrors.address").exists())
                .andExpect(jsonPath("$.validationErrors.paymentMethod").exists())
                .andExpect(jsonPath("$.validationErrors.items").exists());
    }

    private OrderRequest request() {
        return new OrderRequest(
                "Alice",
                "0600000000",
                "Paris",
                "10 rue Exemple",
                "",
                "cash_on_delivery",
                List.of(new OrderItemRequest(1L, 2)),
                new BigDecimal("199.80")
        );
    }
}
