package ru.basted.ithelpdesk.departments.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;

import ru.basted.ithelpdesk.departments.dto.DepartmentsChangeName;
import ru.basted.ithelpdesk.departments.dto.DepartmentsCreateRequest;
import ru.basted.ithelpdesk.departments.dto.DepartmentsResponse;
import ru.basted.ithelpdesk.departments.service.DepartmentsService;

@RestController
@RequestMapping("/api/v1/departments")
@AllArgsConstructor
public class DepartmentsController implements DepartmentsAPI {
    private final DepartmentsService service;

    @Override
    public ResponseEntity<List<DepartmentsResponse>> getAllDepartments() {
        return ResponseEntity.ok(service.getAllDepartments());
    }

    @Override
    public ResponseEntity<DepartmentsResponse> getDepartmentById(Long id) {
        return ResponseEntity.ok(service.getDepartmentById(id));
    }

    @Override
    public ResponseEntity<DepartmentsResponse> createDepartment(DepartmentsCreateRequest createRequest) {
        DepartmentsResponse saved = service.createDepartment(createRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saved);
    }

    @Override
    public ResponseEntity<Void> updateName(Long id, DepartmentsChangeName changeName) {
        service.updateName(id, changeName.name());

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteDepartment(Long id) {
        service.deleteDepartment(id);

        return ResponseEntity.noContent().build();
    }
}
