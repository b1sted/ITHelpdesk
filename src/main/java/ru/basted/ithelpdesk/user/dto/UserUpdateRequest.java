package ru.basted.ithelpdesk.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.basted.ithelpdesk.user.entity.UserRole;

@Schema(description = "Модель данных для обновления информации о пользователе")
public record UserUpdateRequest(
    @NotBlank(message = "Полное имя не может быть пустым")
    @Size(max = 150, message = "Полное имя не может быть длиннее 150 символов")
    @Schema(description = "Полное имя пользователя", example = "Иванов Иван Иванович")
    String fullName,

    @NotBlank(message = "Электронная почта не может быть пустой")
    @Email(message = "Некорректный формат электронной почты")
    @Size(max = 100, message = "Электронная почта не может быть длиннее 100 символов")
    @Schema(description = "Электронная почта пользователя", example = "ivanov.ii@company.ru")
    String email,

    @NotNull(message = "Роль пользователя обязательна")
    @Schema(description = "Роль пользователя", example = "EMPLOYEE")
    UserRole role,

    @NotNull(message = "Идентификатор департамента обязателен")
    @Schema(description = "Идентификатор департамента", example = "1")
    Long departmentId
) {}