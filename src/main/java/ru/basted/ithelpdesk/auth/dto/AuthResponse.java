package ru.basted.ithelpdesk.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель данных с токеном аутентификации и данными пользователя")
public record AuthResponse(
    @Schema(description = "JWT-токен для доступа к API", example = "eyJhbGciOiJSUzI1NiJ9...")
    String token,

    @Schema(description = "Электронная почта пользователя", example = "ivanov.ii@company.ru")
    String email,

    @Schema(description = "Полное имя пользователя", example = "Иванов Иван Иванович")
    String fullName,

    @Schema(description = "Роль пользователя", example = "EMPLOYEE")
    String role
) {}