package ru.basted.ithelpdesk.departments.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import ru.basted.ithelpdesk.departments.dto.DepartmentsCreateRequest;
import ru.basted.ithelpdesk.departments.dto.DepartmentsResponse;
import ru.basted.ithelpdesk.departments.entity.Departments;
import ru.basted.ithelpdesk.departments.mapper.DepartmentsMapper;
import ru.basted.ithelpdesk.departments.repository.DepartmentsRepository;

@Service
@RequiredArgsConstructor
public class DepartmentsServiceImpl implements DepartmentsService {
    private final DepartmentsRepository repository;
    private final DepartmentsMapper mapper;

    @Override
    public List<DepartmentsResponse> getAllDepartments() {
        return repository.findAllByOrderByIdAsc()
                .stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    @Override
    public DepartmentsResponse getDepartmentById(Long id) {
        Departments department = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Не найден департамент с ID " + id));

        return mapper.toResponseDto(department);
    }

    @Override
    public DepartmentsResponse createDepartment(DepartmentsCreateRequest createRequest) {
        if (repository.existsByName(createRequest.name())) {
            throw new IllegalArgumentException(
                    "Департамент с именем " + createRequest.name() + " уже существует в базе данных"
            );
        }

        Departments department = mapper.toEntity(createRequest);
        Departments savedDepartment = repository.save(department);

        return mapper.toResponseDto(savedDepartment);
    }

    @Override
    public void updateName(Long id, String name) {
        Departments departments = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Не найден департамент с ID " + id));
        departments.setName(name);

        repository.save(departments);
    }

    @Override
    public void deleteDepartment(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Не найден департамент с ID " + id);
        }

        repository.deleteById(id);
    }
}
