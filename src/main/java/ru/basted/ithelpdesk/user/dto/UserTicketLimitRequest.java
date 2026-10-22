package ru.basted.ithelpdesk.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель данных для изменения предела заявок специалиста поддержки")
public record UserTicketLimitRequest(
    @NotNull(message = "Предел заявок обязателен")
    @Min(value = 1, message = "Предел заявок должен быть не менее 1")
    @Max(value = 1000, message = "Предел заявок не может превышать 1000")
    @Schema(description = "Максимальное число одновременно взятых в работу заявок", example = "10")
    Integer ticketLimit
) {}
