package ru.basted.ithelpdesk.category.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.basted.ithelpdesk.category.dto.CategoryChangeName;
import ru.basted.ithelpdesk.category.dto.CategoryCreateRequest;
import ru.basted.ithelpdesk.category.dto.CategoryResponse;

@Tag(name = "Категории", description = "Управление категориями инцидентов")
public interface CategoryAPI {
    @Operation(
            summary = "Получить список всех категорий",
            description = "Возвращает список всех зарегистрированных категорий инцидентов"
    )
    @GetMapping
    ResponseEntity<List<CategoryResponse>> getAllCategories();

    @Operation(
            summary = "Получить информацию о категории по её уникальному идентификатору",
            description = "Возвращает информацию о конкретной категории, если она зарегистрирована в базе данных, " +
                    "по её уникальному идентификатору"
    )
    @GetMapping("/{id}")
    ResponseEntity<CategoryResponse> getCategoryById(
            @Parameter(description = "Уникальный идентификатор категории", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "ID не может быть меньше нуля") Long id
    );

    @Operation(
            summary = "Создать новую категорию",
            description = "Создание записи в базе данных о новой категории инцидентов. Доступно только администраторам"
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest createRequest
    );

    @Operation(
            summary = "Смена имени категории",
            description = "Изменение имени существующей категории. Доступна только администраторам."
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> updateName(
            @Parameter(description = "Уникальный идентификатор категории", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "ID не может быть меньше нуля") Long id,

            @Valid @RequestBody CategoryChangeName changeName
    );

    @Operation(
            summary = "Удаление категории",
            description = "Удаление категории. Доступна только администраторам."
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteCategory(
            @Parameter(description = "Уникальный идентификатор категории", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "ID не может быть меньше нуля") Long id
    );
}