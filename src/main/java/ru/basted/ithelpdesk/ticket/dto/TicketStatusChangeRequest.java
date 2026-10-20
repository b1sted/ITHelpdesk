package ru.basted.ithelpdesk.ticket.dto;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.basted.ithelpdesk.ticket.entity.TicketStatus;

@Schema(description = "Модель данных для изменения статуса заявки")
public record TicketStatusChangeRequest(
    @NotNull(message = "Новый статус заявки обязателен")
    @Schema(description = "Новый статус заявки", example = "RESOLVED")
    TicketStatus status,

    @Schema(description = "Причина, поясняющая изменение статуса (обязательна при возврате на доработку)",
            example = "Проблема воспроизводится повторно")
    String reason
) {}