package ru.basted.ithelpdesk.department.service;

import java.util.List;

import ru.basted.ithelpdesk.department.dto.DepartmentCreateRequest;
import ru.basted.ithelpdesk.department.dto.DepartmentResponse;

public interface DepartmentService {
    List<DepartmentResponse> getAllDepartments();
    DepartmentResponse getDepartmentById(Long id);

    DepartmentResponse createDepartment(DepartmentCreateRequest createRequest);
    void updateName(Long id, String name);
    void deleteDepartment(Long id);
}