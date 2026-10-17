package ru.basted.ithelpdesk.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Краткая информация о пользователе для вложенных представлений")
public record UserSummary(
    @Schema(description = "Уникальный идентификатор пользователя", example = "1")
    Long id,

    @Schema(description = "Полное имя пользователя", example = "Иванов Иван Иванович")
    String fullName,

    @Schema(description = "Электронная почта пользователя", example = "ivanov.ii@company.ru")
    String email,

    @Schema(description = "Название департамента пользователя", example = "Департамент разработки")
    String departmentName
) {}