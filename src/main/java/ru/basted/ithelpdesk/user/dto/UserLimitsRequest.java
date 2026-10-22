package ru.basted.ithelpdesk.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель данных для изменения лимитов специалиста поддержки")
public record UserLimitsRequest(
    @NotNull(message = "Предел заявок обязателен")
    @Min(value = 1, message = "Предел заявок должен быть не менее 1")
    @Max(value = 1000, message = "Предел заявок не может превышать 1000")
    @Schema(description = "Максимальное число одновременно взятых в работу заявок", example = "10")
    Integer ticketLimit,

    @NotNull(message = "Лимит времени обязателен")
    @Min(value = 1, message = "Лимит времени должен быть не менее 1 часа")
    @Max(value = 720, message = "Лимит времени не может превышать 720 часов")
    @Schema(description = "Предельное время выполнения назначенной заявки в часах", example = "24")
    Integer timeLimitHours
) {}
