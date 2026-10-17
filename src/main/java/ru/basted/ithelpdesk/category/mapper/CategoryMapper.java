package ru.basted.ithelpdesk.category.mapper;

import org.mapstruct.Mapper;

import ru.basted.ithelpdesk.category.dto.CategoryCreateRequest;
import ru.basted.ithelpdesk.category.dto.CategoryResponse;
import ru.basted.ithelpdesk.category.entity.Category;

@Mapper
public interface CategoryMapper {
    Category toEntity(CategoryCreateRequest dto);

    CategoryResponse toResponseDto(Category category);
}