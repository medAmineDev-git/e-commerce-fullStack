package com.ecommerce.backend.product;

import com.ecommerce.backend.product.dto.ProductRequest;
import com.ecommerce.backend.product.dto.ProductPageResponse;
import com.ecommerce.backend.product.dto.ProductResponse;
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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.ecommerce.backend.common.exception.GlobalExceptionHandler.class)
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private com.ecommerce.backend.store.StoreService storeService;

    @Test
    void getAllShouldReturn200AndList() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(
                response(1L, "Sneaker"),
                response(2L, "T-shirt")
        ));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Sneaker"))
                .andExpect(jsonPath("$[1].id").value(2));
    }

        @Test
        void searchPageShouldReturn200AndPagePayload() throws Exception {
        ProductPageResponse page = new ProductPageResponse(
            List.of(response(1L, "Sneaker")),
            0,
            12,
            1,
            1,
            true,
            "price",
            "asc",
            "sneaker",
            "Sneakers"
        );

        when(productService.searchProducts("sneaker", "Sneakers", "", "", 0, 12, "price", "asc")).thenReturn(page);

        mockMvc.perform(get("/api/products/page")
                .param("q", "sneaker")
                .param("category", "Sneakers")
                .param("page", "0")
                .param("size", "12")
                .param("sortBy", "price")
                .param("sortDirection", "asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].name").value("Sneaker"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.sortBy").value("price"))
            .andExpect(jsonPath("$.sortDirection").value("asc"))
            .andExpect(jsonPath("$.category").value("Sneakers"));
        }

    @Test
    void getByIdShouldReturn200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(response(1L, "Sneaker"));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Sneaker"));
    }

    @Test
    void getByIdShouldReturn404WhenNotFound() throws Exception {
        when(productService.getProductById(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Produit introuvable avec l'id 99"));
    }

    @Test
    void createShouldReturn201WhenPayloadIsValid() throws Exception {
        ProductRequest request = request("Sneaker");
        when(productService.createProduct(request)).thenReturn(response(10L, "Sneaker"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Sneaker"));
    }

    @Test
    void createShouldReturn400WhenPayloadIsInvalid() throws Exception {
        ProductRequest invalid = new ProductRequest("", "", "", new BigDecimal("0"), -1);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.description").exists())
                .andExpect(jsonPath("$.validationErrors.category").exists())
                .andExpect(jsonPath("$.validationErrors.price").exists())
                .andExpect(jsonPath("$.validationErrors.stockQuantity").exists());
    }

    @Test
    void updateShouldReturn200WhenPayloadIsValid() throws Exception {
        ProductRequest request = request("Updated Sneaker");
        when(productService.updateProduct(1L, request)).thenReturn(response(1L, "Updated Sneaker"));

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Sneaker"));
    }

    @Test
    void deleteShouldReturn204() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }

    @Test
    void deleteShouldReturn404WhenNotFound() throws Exception {
        doThrow(new ProductNotFoundException(404L)).when(productService).deleteProduct(404L);

        mockMvc.perform(delete("/api/products/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Produit introuvable avec l'id 404"));
    }

    private ProductRequest request(String name) {
        return new ProductRequest(
                name,
                "Sneakers",
                "Description",
                new BigDecimal("29.90"),
                8
        );
    }

    private ProductResponse response(Long id, String name) {
        return new ProductResponse(
                id,
                name,
                "Sneakers",
                "Description",
                new BigDecimal("29.90"),
                8
        );
    }
}
