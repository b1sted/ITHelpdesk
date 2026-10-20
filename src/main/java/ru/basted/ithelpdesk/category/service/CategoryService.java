package ru.basted.ithelpdesk.category.service;

import java.util.List;

import ru.basted.ithelpdesk.category.dto.CategoryCreateRequest;
import ru.basted.ithelpdesk.category.dto.CategoryResponse;

public interface CategoryService {
    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(Long id);

    CategoryResponse createCategory(CategoryCreateRequest createRequest);

    CategoryResponse updateName(Long id, String name);

    void deleteCategory(Long id);
}