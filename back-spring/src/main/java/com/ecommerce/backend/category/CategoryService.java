package com.ecommerce.backend.category;

import com.ecommerce.backend.category.dto.CategoryRequest;
import com.ecommerce.backend.category.dto.CategoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream().map(categoryMapper::toResponse).toList();
    }

    public CategoryResponse getCategoryById(Long id) {
        return categoryMapper.toResponse(findByIdOrThrow(id));
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        Category created = categoryMapper.toEntity(request);
        applyParent(created, request.parentId());
        created = categoryRepository.save(created);
        return categoryMapper.toResponse(created);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category existing = findByIdOrThrow(id);
        categoryMapper.updateEntity(existing, request);
        applyParent(existing, request.parentId());
        Category updated = categoryRepository.save(existing);
        return categoryMapper.toResponse(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.delete(findByIdOrThrow(id));
    }

    private Category findByIdOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    private void applyParent(Category category, Long parentId) {
        if (parentId == null) {
            category.setParent(null);
            return;
        }
        Category parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent category does not exist: " + parentId));
        if (category.getId() != null && category.getId().equals(parentId)) {
            throw new IllegalArgumentException("A category cannot be its own parent");
        }
        category.setParent(parent);
    }
}
