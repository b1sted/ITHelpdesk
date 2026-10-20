package ru.basted.ithelpdesk.auth.controller;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
            @ApiResponse(responseCode = "200",
                    description = "Успешная аутентификация, возвращает JWT-токен",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "token": "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJpdGhlbHBkZXNrIiwic3ViIjoiaXZhbm92LmlpQGNvbXBhbnkucnUiLCJyb2xlIjoiRU1QTE9ZRUUiLCJleHAiOjE3OTIzNjY4NTksImlhdCI6MTc5MjI4MDQ1OX0.ExampleSignature",
                                      "email": "ivanov.ii@company.ru",
                                      "fullName": "Иванов Иван Иванович",
                                      "role": "EMPLOYEE"
                                    }"""))),
            @ApiResponse(responseCode = "400",
                    description = "Некорректные данные запроса",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = "{\"message\":\"Электронная почта не может быть пустой\"}"))),
            @ApiResponse(responseCode = "404",
                    description = "Неверные учётные данные",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = "{\"message\":\"Неверные учётные данные\"}"))),
            @ApiResponse(responseCode = "429",
                    description = "Превышен лимит попыток входа",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Слишком много попыток входа. Попробуйте позже.\"}")))
    })
    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest loginRequest
    );
}