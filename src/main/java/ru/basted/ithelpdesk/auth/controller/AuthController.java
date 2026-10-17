package ru.basted.ithelpdesk.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.basted.ithelpdesk.auth.dto.AuthResponse;
import ru.basted.ithelpdesk.auth.dto.LoginRequest;
import ru.basted.ithelpdesk.auth.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthAPI {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<AuthResponse> login(LoginRequest loginRequest) {
        return ResponseEntity.ok(service.login(loginRequest));
    }
}