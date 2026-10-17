package ru.basted.ithelpdesk.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель данных для аутентификации пользователя")
public record LoginRequest(
    @NotBlank(message = "Электронная почта не может быть пустой")
    @Email(message = "Некорректный формат электронной почты")
    @Schema(description = "Электронная почта пользователя", example = "ivanov.ii@company.ru")
    String email,

    @NotBlank(message = "Пароль не может быть пустым")
    @Schema(description = "Пароль пользователя", example = "P@ssw0rd2024")
    String password
) {}