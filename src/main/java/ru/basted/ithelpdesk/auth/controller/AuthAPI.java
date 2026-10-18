package ru.basted.ithelpdesk.auth.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.basted.ithelpdesk.auth.dto.AuthResponse;
import ru.basted.ithelpdesk.auth.dto.LoginRequest;
import ru.basted.ithelpdesk.core.exception.ApiError;

@Tag(name = "Аутентификация", description = "Вход в систему")
public interface AuthAPI {
    @Operation(
            summary = "Аутентификация пользователя",
            description = "Проверяет учётные данные и возвращает JWT-токен для доступа к API."
                    + " Полученный токен необходимо передавать в заголовке Authorization: Bearer <token>"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешная аутентификация, возвращает JWT-токен",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Неверные учётные данные",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest loginRequest
    );
}