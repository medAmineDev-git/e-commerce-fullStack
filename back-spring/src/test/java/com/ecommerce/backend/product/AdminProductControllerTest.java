package com.ecommerce.backend.product;

import com.ecommerce.backend.product.dto.ProductRequest;
import com.ecommerce.backend.product.dto.ProductResponse;
import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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

/**
 * Couche web du back-office produits : codes de retour, validation des charges
 * utiles et traduction des erreurs. L'isolation entre boutiques, elle, est
 * couverte de bout en bout par StoreIsolationTest.
 */
@WebMvcTest(AdminProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.ecommerce.backend.common.exception.GlobalExceptionHandler.class)
@ActiveProfiles("test")
class AdminProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private StoreContext storeContext;

    private Store store;

    @BeforeEach
    void setUp() {
        store = new Store();
        store.setId(1L);
        store.setName("NOVA Boutique Urbaine");
        store.setSlug("nova");
        when(storeContext.requireOwnedStore(any())).thenReturn(store);
    }

    @Test
    void getAllShouldReturn200AndList() throws Exception {
        when(productService.getAllProducts(store)).thenReturn(List.of(
                response(1L, "Sneaker"),
                response(2L, "T-shirt")
        ));

        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Sneaker"))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void getByIdShouldReturn200() throws Exception {
        when(productService.getProductById(store, 1L)).thenReturn(response(1L, "Sneaker"));

        mockMvc.perform(get("/api/admin/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getByIdShouldReturn404WhenNotFoundInThisStore() throws Exception {
        when(productService.getProductById(store, 99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/admin/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Produit introuvable avec l'id 99"));
    }

    @Test
    void createShouldReturn201WhenPayloadIsValid() throws Exception {
        ProductRequest request = request("Cap");
        when(productService.createProduct(store, request)).thenReturn(response(10L, "Cap"));

        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Cap"));
    }

    @Test
    void createShouldReturn400WhenPayloadIsInvalid() throws Exception {
        ProductRequest invalid = new ProductRequest("", "", "", new BigDecimal("-1.00"), -5);

        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.stockQuantity").exists())
                // Categorie et description vides sont acceptees : elles sont facultatives.
                .andExpect(jsonPath("$.validationErrors.category").doesNotExist())
                .andExpect(jsonPath("$.validationErrors.description").doesNotExist());
    }

    @Test
    void createShouldAcceptAProductWithoutCategoryNorDescription() throws Exception {
        ProductRequest request = new ProductRequest("Cap", null, null, new BigDecimal("19.90"), 5);
        when(productService.createProduct(store, request)).thenReturn(response(10L, "Cap"));

        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cap"));
    }

    @Test
    void createShouldReturn400WhenBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void updateShouldReturn200WhenPayloadIsValid() throws Exception {
        ProductRequest request = request("Updated");
        when(productService.updateProduct(store, 1L, request)).thenReturn(response(1L, "Updated"));

        mockMvc.perform(put("/api/admin/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void updateShouldReturn404WhenProductBelongsToAnotherStore() throws Exception {
        ProductRequest request = request("Updated");
        when(productService.updateProduct(store, 77L, request)).thenThrow(new ProductNotFoundException(77L));

        mockMvc.perform(put("/api/admin/products/77")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteShouldReturn204() throws Exception {
        doNothing().when(productService).deleteProduct(store, 1L);

        mockMvc.perform(delete("/api/admin/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(store, 1L);
    }

    @Test
    void deleteShouldReturn404WhenProductBelongsToAnotherStore() throws Exception {
        doThrow(new ProductNotFoundException(404L)).when(productService).deleteProduct(store, 404L);

        mockMvc.perform(delete("/api/admin/products/404"))
                .andExpect(status().isNotFound());
    }

    private ProductRequest request(String name) {
        return new ProductRequest(name, "Sneakers", "Description", new BigDecimal("29.90"), 12);
    }

    private ProductResponse response(Long id, String name) {
        return new ProductResponse(id, name, "Sneakers", "Description", new BigDecimal("29.90"), 12);
    }
}
