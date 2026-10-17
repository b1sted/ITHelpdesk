package ru.basted.ithelpdesk.category.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.basted.ithelpdesk.category.dto.CategoryChangeName;
import ru.basted.ithelpdesk.category.dto.CategoryCreateRequest;
import ru.basted.ithelpdesk.category.dto.CategoryResponse;
import ru.basted.ithelpdesk.category.service.CategoryService;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController implements CategoryAPI {
    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(service.getAllCategories());
    }

    @Override
    public ResponseEntity<CategoryResponse> getCategoryById(Long id) {
        return ResponseEntity.ok(service.getCategoryById(id));
    }

    @Override
    public ResponseEntity<CategoryResponse> createCategory(CategoryCreateRequest createRequest) {
        CategoryResponse saved = service.createCategory(createRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saved);
    }

    @Override
    public ResponseEntity<Void> updateName(Long id, CategoryChangeName changeName) {
        service.updateName(id, changeName.name());

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCategory(Long id) {
        service.deleteCategory(id);

        return ResponseEntity.noContent().build();
    }
}