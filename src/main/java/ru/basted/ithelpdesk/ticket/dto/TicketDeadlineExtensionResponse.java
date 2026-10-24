package ru.basted.ithelpdesk.ticket.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.basted.ithelpdesk.user.dto.UserSummary;

@Schema(description = "Запись о продлении крайнего срока заявки")
public record TicketDeadlineExtensionResponse(
    @Schema(description = "Уникальный идентификатор записи", example = "1")
    Long id,

    @Schema(description = "На сколько часов продлён срок", example = "24")
    Integer hours,

    @Schema(description = "Причина продления", example = "Ожидается согласование доступа")
    String reason,

    @Schema(description = "Дата и время продления", example = "2026-10-18T10:15:30")
    LocalDateTime createdAt,

    @Schema(description = "Пользователь, продливший срок")
    UserSummary createdBy
) {}
