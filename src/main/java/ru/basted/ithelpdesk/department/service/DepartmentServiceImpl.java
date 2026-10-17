package ru.basted.ithelpdesk.department.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import ru.basted.ithelpdesk.department.dto.DepartmentCreateRequest;
import ru.basted.ithelpdesk.department.dto.DepartmentResponse;
import ru.basted.ithelpdesk.department.entity.Department;
import ru.basted.ithelpdesk.department.mapper.DepartmentMapper;
import ru.basted.ithelpdesk.department.repository.DepartmentRepository;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository repository;
    private final DepartmentMapper mapper;

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
        Department department = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Не найден департамент с ID " + id));

        return mapper.toResponseDto(department);
    }

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentCreateRequest createRequest) {
        if (repository.existsByName(createRequest.name())) {
            throw new IllegalArgumentException(
                    "Департамент с именем " + createRequest.name() + " уже существует в базе данных"
            );
        }

        Department department = mapper.toEntity(createRequest);
        Department savedDepartment = repository.save(department);

        return mapper.toResponseDto(savedDepartment);
    }

    @Override
    @Transactional
    public void updateName(Long id, String name) {
        Department department = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Не найден департамент с ID " + id));
        department.setName(name);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Не найден департамент с ID " + id);
        }

        repository.deleteById(id);
    }
}