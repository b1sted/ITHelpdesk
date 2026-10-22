package ru.basted.ithelpdesk.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.basted.ithelpdesk.user.entity.UserRole;

@Schema(description = "Модель данных, возвращаемая при создании или получении информации о пользователе")
public record UserResponse(
    @Schema(description = "Уникальный идентификатор пользователя", example = "1")
    Long id,

    @Schema(description = "Полное имя пользователя", example = "Иванов Иван Иванович")
    String fullName,

    @Schema(description = "Электронная почта пользователя", example = "ivanov.ii@company.ru")
    String email,

    @Schema(description = "Роль пользователя", example = "EMPLOYEE")
    UserRole role,

    @Schema(description = "Название департамента пользователя", example = "Департамент разработки")
    String departmentName,

    @Schema(description = "Предел одновременно взятых в работу заявок для специалиста", example = "10")
    Integer ticketLimit,

    @Schema(description = "Предельное время выполнения назначенной заявки в часах", example = "24")
    Integer timeLimitHours
) {}
