package ru.basted.ithelpdesk.category.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
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
        return mapper.toResponseDto(findCategory(id));
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest createRequest) {
        if (repository.existsByName(createRequest.name())) {
            throw new ConflictException(
                    "Категория с именем %s уже существует в базе данных"
                            .formatted(createRequest.name())
            );
        }

        return mapper.toResponseDto(repository.save(mapper.toEntity(createRequest)));
    }

    @Override
    @Transactional
    public CategoryResponse updateName(Long id, String name) {
        Category category = findCategory(id);

        if (repository.existsByNameAndIdNot(name, id)) {
            throw new ConflictException(
                    "Категория с именем %s уже существует в базе данных".formatted(name)
            );
        }

        category.setName(name);
        return mapper.toResponseDto(repository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategory(id);

        try {
            repository.delete(category);
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(
                    "Нельзя удалить категорию, к которой привязаны заявки"
            );
        }
    }

    private Category findCategory(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Не найдена категория с ID %d".formatted(id)
                ));
    }
}
