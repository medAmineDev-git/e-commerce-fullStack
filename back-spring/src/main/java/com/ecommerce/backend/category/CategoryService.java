package com.ecommerce.backend.category;

import com.ecommerce.backend.category.dto.CategoryRequest;
import com.ecommerce.backend.category.dto.CategoryResponse;
import com.ecommerce.backend.store.Store;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Toutes les operations sont bornees a une boutique. L'unicite du nom de categorie
 * vaut a l'interieur d'une boutique, pas sur la plateforme : deux boutiques peuvent
 * chacune avoir une categorie "Sneakers".
 */
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryResponse> getAllCategories(Store store) {
        return categoryRepository.findAllByStoreOrderByIdAsc(store).stream()
                .map(categoryMapper::toResponse)
                .toList();
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
    public void deleteCategory(Store store, Long id) {
        Category existing = findByIdAndStoreOrThrow(id, store);
        categoryRepository.delete(existing);
    }

    private Category findByIdAndStoreOrThrow(Long id, Store store) {
        return categoryRepository.findByIdAndStore(id, store)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    /**
     * Le parent est cherche dans la meme boutique : rattacher une categorie a celle
     * d'une autre boutique creerait une hierarchie a cheval sur deux perimetres.
     */
    private void applyParent(Category category, Long parentId, Store store) {
        if (parentId == null) {
            category.setParent(null);
            return;
        }

        Category parent = categoryRepository.findByIdAndStore(parentId, store)
                .orElseThrow(() -> new IllegalArgumentException("Parent category does not exist in this store: " + parentId));

        if (category.getId() != null && category.getId().equals(parentId)) {
            throw new IllegalArgumentException("A category cannot be its own parent");
        }
        category.setParent(parent);
    }
}
