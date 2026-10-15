package ru.basted.ithelpdesk.departments.service;

import java.util.List;

import ru.basted.ithelpdesk.departments.dto.DepartmentsCreateRequest;
import ru.basted.ithelpdesk.departments.dto.DepartmentsResponse;

public interface DepartmentsService {
    List<DepartmentsResponse> getAllDepartments();
    DepartmentsResponse getDepartmentById(Long id);

    DepartmentsResponse createDepartment(DepartmentsCreateRequest createRequest);
    void updateName(Long id, String name);
    void deleteDepartment(Long id);
}
