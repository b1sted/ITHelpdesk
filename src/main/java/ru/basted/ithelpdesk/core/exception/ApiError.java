package ru.basted.ithelpdesk.core.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель данных об ошибке")
public record ApiError(
    @Schema(description = "Сообщение об ошибке", example = "Не найдена заявка с ID 1")
    String message
) {}