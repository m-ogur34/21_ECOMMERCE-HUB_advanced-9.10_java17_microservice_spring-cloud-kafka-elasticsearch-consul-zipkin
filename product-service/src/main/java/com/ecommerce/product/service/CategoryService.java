package com.ecommerce.product.service;

import com.ecommerce.common.constants.CacheConstants;
import com.ecommerce.common.dto.product.CategoryRequest;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.product.model.Category;
import com.ecommerce.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Kategori yönetim servisi */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.CATEGORIES)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.CATEGORY, key = "#id")
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    @CacheEvict(value = {CacheConstants.CATEGORIES, CacheConstants.CATEGORY}, allEntries = true)
    public Category createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BusinessException("Bu kategori adı zaten kullanılıyor: " + request.getName());
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getParentId()));
            category.setParent(parent);
        }

        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<Category> getRootCategories() {
        return categoryRepository.findRootCategories();
    }
}
