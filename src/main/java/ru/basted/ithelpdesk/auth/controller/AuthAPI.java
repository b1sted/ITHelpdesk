package ru.basted.ithelpdesk.auth.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.basted.ithelpdesk.auth.dto.AuthResponse;
import ru.basted.ithelpdesk.auth.dto.LoginRequest;

@Tag(name = "Аутентификация", description = "Вход в систему")
public interface AuthAPI {
    @Operation(
            summary = "Аутентификация пользователя",
            description = "Проверяет учётные данные и возвращает JWT-токен для доступа к API"
    )
    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest loginRequest
    );
}