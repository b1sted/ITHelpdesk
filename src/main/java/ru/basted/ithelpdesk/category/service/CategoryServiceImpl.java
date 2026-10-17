package ru.basted.ithelpdesk.category.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.basted.ithelpdesk.category.dto.CategoryCreateRequest;
import ru.basted.ithelpdesk.category.dto.CategoryResponse;
import ru.basted.ithelpdesk.category.entity.Category;
import ru.basted.ithelpdesk.category.mapper.CategoryMapper;
import ru.basted.ithelpdesk.category.repository.CategoryRepository;
import ru.basted.ithelpdesk.core.exception.ConflictException;
import ru.basted.ithelpdesk.core.exception.NotFoundException;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repository;
    private final CategoryMapper mapper;

    public CategoryServiceImpl(CategoryRepository repository, CategoryMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return repository.findAllByOrderByIdAsc()
                .stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Не найдена категория с ID " + id));

        return mapper.toResponseDto(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest createRequest) {
        if (repository.existsByName(createRequest.name())) {
            throw new ConflictException(
                    "Категория с именем " + createRequest.name() + " уже существует в базе данных"
            );
        }

        Category category = mapper.toEntity(createRequest);
        Category savedCategory = repository.save(category);

        return mapper.toResponseDto(savedCategory);
    }

    @Override
    @Transactional
    public void updateName(Long id, String name) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Не найдена категория с ID " + id));
        category.setName(name);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Не найдена категория с ID " + id);
        }

        repository.deleteById(id);
    }
}