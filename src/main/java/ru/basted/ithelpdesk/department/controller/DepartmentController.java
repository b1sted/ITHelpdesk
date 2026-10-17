package ru.basted.ithelpdesk.department.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.basted.ithelpdesk.department.dto.DepartmentChangeName;
import ru.basted.ithelpdesk.department.dto.DepartmentCreateRequest;
import ru.basted.ithelpdesk.department.dto.DepartmentResponse;
import ru.basted.ithelpdesk.department.service.DepartmentService;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController implements DepartmentAPI {
    private final DepartmentService service;

    public DepartmentController(DepartmentService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        return ResponseEntity.ok(service.getAllDepartments());
    }

    @Override
    public ResponseEntity<DepartmentResponse> getDepartmentById(Long id) {
        return ResponseEntity.ok(service.getDepartmentById(id));
    }

    @Override
    public ResponseEntity<DepartmentResponse> createDepartment(DepartmentCreateRequest createRequest) {
        DepartmentResponse saved = service.createDepartment(createRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saved);
    }

    @Override
    public ResponseEntity<Void> updateName(Long id, DepartmentChangeName changeName) {
        service.updateName(id, changeName.name());

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteDepartment(Long id) {
        service.deleteDepartment(id);

        return ResponseEntity.noContent().build();
    }
}