package ru.basted.ithelpdesk.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.basted.ithelpdesk.user.entity.UserRole;

@Schema(description = "Модель данных для частичного обновления информации о пользователе")
public record UserUpdateRequest(
    @Schema(description = "Полное имя пользователя", example = "Иванов Иван Иванович")
    String fullName,

    @Schema(description = "Электронная почта пользователя", example = "ivanov.ii@company.ru")
    String email,

    @Schema(description = "Роль пользователя", example = "EMPLOYEE")
    UserRole role,

    @Schema(description = "Идентификатор департамента", example = "1")
    Long departmentId
) {}
