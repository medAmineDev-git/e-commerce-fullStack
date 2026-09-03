package com.ecommerce.backend.category;

import com.ecommerce.backend.category.dto.CategoryRequest;
import com.ecommerce.backend.category.dto.CategoryResponse;
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

@WebMvcTest(AdminCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.ecommerce.backend.common.exception.GlobalExceptionHandler.class)
@ActiveProfiles("test")
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

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
        when(categoryService.getAllCategories(store)).thenReturn(List.of(
                response(1L, "Homme"),
                response(2L, "Femme")
        ));

        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Homme"))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void getByIdShouldReturn404WhenNotFoundInThisStore() throws Exception {
        when(categoryService.getCategoryById(store, 99L)).thenThrow(new CategoryNotFoundException(99L));

        mockMvc.perform(get("/api/admin/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Categorie introuvable avec l'id 99"));
    }

    @Test
    void createShouldReturn201WhenPayloadIsValid() throws Exception {
        CategoryRequest request = request("Sneakers");
        when(categoryService.createCategory(store, request)).thenReturn(response(10L, "Sneakers"));

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Sneakers"));
    }

    @Test
    void createShouldReturn400WhenPayloadIsInvalid() throws Exception {
        CategoryRequest invalid = new CategoryRequest("", "");

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.description").exists());
    }

    @Test
    void createShouldReturn400WhenNameAlreadyExistsInTheStore() throws Exception {
        CategoryRequest request = request("Sneakers");
        when(categoryService.createCategory(store, request))
                .thenThrow(new IllegalArgumentException("Category name already exists in this store: Sneakers"));

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Category name already exists in this store: Sneakers"));
    }

    @Test
    void updateShouldReturn200WhenPayloadIsValid() throws Exception {
        CategoryRequest request = request("Accessoires");
        when(categoryService.updateCategory(store, 1L, request)).thenReturn(response(1L, "Accessoires"));

        mockMvc.perform(put("/api/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Accessoires"));
    }

    @Test
    void deleteShouldReturn204() throws Exception {
        doNothing().when(categoryService).deleteCategory(store, 1L);

        mockMvc.perform(delete("/api/admin/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(store, 1L);
    }

    @Test
    void deleteShouldReturn404WhenCategoryBelongsToAnotherStore() throws Exception {
        doThrow(new CategoryNotFoundException(404L)).when(categoryService).deleteCategory(store, 404L);

        mockMvc.perform(delete("/api/admin/categories/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Categorie introuvable avec l'id 404"));
    }

    private CategoryRequest request(String name) {
        return new CategoryRequest(name, "Description");
    }

    private CategoryResponse response(Long id, String name) {
        return new CategoryResponse(id, name, "Description");
    }
}
