package ru.basted.ithelpdesk.ticket.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.basted.ithelpdesk.user.dto.UserSummary;

@Schema(description = "Запись о закрытии заявки специалистом без решения")
public record TicketClosureResponse(
    @Schema(description = "Уникальный идентификатор записи", example = "1")
    Long id,

    @Schema(description = "Причина закрытия заявки",
            example = "Требуется замена оборудования, недоступная в рамках текущего бюджета")
    String reason,

    @Schema(description = "Дата и время закрытия заявки", example = "2026-10-18T10:15:30")
    LocalDateTime createdAt,

    @Schema(description = "Специалист, закрывший заявку")
    UserSummary closedBy
) {}
