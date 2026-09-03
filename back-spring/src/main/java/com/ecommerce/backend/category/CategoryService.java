package com.ecommerce.backend.category;

import com.ecommerce.backend.category.dto.CategoryRequest;
import com.ecommerce.backend.category.dto.CategoryResponse;
import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final StoreRepository storeRepository;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper, StoreRepository storeRepository) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.storeRepository = storeRepository;
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream().map(categoryMapper::toResponse).toList();
    }

    public List<CategoryResponse> getAllCategories(Store store) {
        return categoryRepository.findAllByStoreOrderByIdAsc(store).stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    public CategoryResponse getCategoryById(Long id) {
        return categoryMapper.toResponse(findByIdOrThrow(id));
    }

    public CategoryResponse getCategoryById(Store store, Long id) {
        return categoryMapper.toResponse(findByIdAndStoreOrThrow(id, store));
    }

    @Transactional
    public CategoryResponse createCategory(Store store, CategoryRequest request) {
        if (categoryRepository.existsByStoreAndNameIgnoreCase(store, request.name().trim())) {
            throw new IllegalArgumentException("Category name already exists in this store: " + request.name());
        }

        Category created = categoryMapper.toEntity(request);
        created.setStore(store);
        applyParent(created, request.parentId(), store);
        created = categoryRepository.save(created);
        return categoryMapper.toResponse(created);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        Store defaultStore = getDefaultStore();
        return createCategory(defaultStore, request);
    }

    @Transactional
    public CategoryResponse updateCategory(Store store, Long id, CategoryRequest request) {
        Category existing = findByIdAndStoreOrThrow(id, store);
        if (categoryRepository.existsByStoreAndNameIgnoreCaseAndIdNot(store, request.name().trim(), id)) {
            throw new IllegalArgumentException("Category name already exists in this store: " + request.name());
        }

        categoryMapper.updateEntity(existing, request);
        applyParent(existing, request.parentId(), store);
        Category updated = categoryRepository.save(existing);
        return categoryMapper.toResponse(updated);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category existing = findByIdOrThrow(id);
        categoryMapper.updateEntity(existing, request);
        applyParent(existing, request.parentId(), existing.getStore());
        Category updated = categoryRepository.save(existing);
        return categoryMapper.toResponse(updated);
    }

    @Transactional
    public void deleteCategory(Store store, Long id) {
        Category existing = findByIdAndStoreOrThrow(id, store);
        categoryRepository.delete(existing);
    }

    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.delete(findByIdOrThrow(id));
    }

    private Category findByIdOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    private Category findByIdAndStoreOrThrow(Long id, Store store) {
        return categoryRepository.findByIdAndStore(id, store)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    private void applyParent(Category category, Long parentId, Store store) {
        if (parentId == null) {
            category.setParent(null);
            return;
        }
        Category parent = store != null
                ? categoryRepository.findByIdAndStore(parentId, store)
                        .orElseThrow(() -> new IllegalArgumentException("Parent category does not exist in this store: " + parentId))
                : categoryRepository.findById(parentId)
                        .orElseThrow(() -> new IllegalArgumentException("Parent category does not exist: " + parentId));

        if (category.getId() != null && category.getId().equals(parentId)) {
            throw new IllegalArgumentException("A category cannot be its own parent");
        }
        category.setParent(parent);
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
