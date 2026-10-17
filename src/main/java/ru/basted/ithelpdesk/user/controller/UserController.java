package ru.basted.ithelpdesk.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.basted.ithelpdesk.user.dto.UserCreateRequest;
import ru.basted.ithelpdesk.user.dto.UserResponse;
import ru.basted.ithelpdesk.user.dto.UserUpdateRequest;
import ru.basted.ithelpdesk.user.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController implements UserAPI {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(service.getAllUsers());
    }

    @Override
    public ResponseEntity<UserResponse> getUserById(Long id) {
        return ResponseEntity.ok(service.getUserById(id));
    }

    @Override
    public ResponseEntity<UserResponse> createUser(UserCreateRequest createRequest) {
        UserResponse saved = service.createUser(createRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saved);
    }

    @Override
    public ResponseEntity<UserResponse> updateUser(Long id, UserUpdateRequest updateRequest) {
        return ResponseEntity.ok(service.updateUser(id, updateRequest));
    }

    @Override
    public ResponseEntity<Void> deleteUser(Long id) {
        service.deleteUser(id);

        return ResponseEntity.noContent().build();
    }
}