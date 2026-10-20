package ru.basted.ithelpdesk.department.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.basted.ithelpdesk.core.exception.ConflictException;
import ru.basted.ithelpdesk.core.exception.NotFoundException;
import ru.basted.ithelpdesk.department.dto.DepartmentCreateRequest;
import ru.basted.ithelpdesk.department.dto.DepartmentResponse;
import ru.basted.ithelpdesk.department.entity.Department;
import ru.basted.ithelpdesk.department.mapper.DepartmentMapper;
import ru.basted.ithelpdesk.department.repository.DepartmentRepository;

@Service
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository repository;
    private final DepartmentMapper mapper;

    public DepartmentServiceImpl(DepartmentRepository repository, DepartmentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return repository.findAllByOrderByIdAsc()
                .stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        return mapper.toResponseDto(findDepartment(id));
    }

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentCreateRequest createRequest) {
        if (repository.existsByName(createRequest.name())) {
            throw new ConflictException(
                    "Департамент с именем %s уже существует в базе данных"
                            .formatted(createRequest.name())
            );
        }

        return mapper.toResponseDto(repository.save(mapper.toEntity(createRequest)));
    }

    @Override
    @Transactional
    public DepartmentResponse updateName(Long id, String name) {
        Department department = findDepartment(id);

        if (repository.existsByNameAndIdNot(name, id)) {
            throw new ConflictException(
                    "Департамент с именем %s уже существует в базе данных".formatted(name)
            );
        }

        department.setName(name);
        return mapper.toResponseDto(repository.save(department));
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        Department department = findDepartment(id);

        try {
            repository.delete(department);
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(
                    "Нельзя удалить департамент, к которому привязаны пользователи"
            );
        }
    }

    private Department findDepartment(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Не найден департамент с ID %d".formatted(id)
                ));
    }
}
