package ru.basted.ithelpdesk.ticketrequest.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.basted.ithelpdesk.ticketrequest.entity.TicketRequestStatus;
import ru.basted.ithelpdesk.ticketrequest.entity.TicketRequestType;
import ru.basted.ithelpdesk.user.dto.UserSummary;

@Schema(description = "Модель данных запроса отказа или переназначения заявки")
public record TicketRequestResponse(
    @Schema(description = "Уникальный идентификатор запроса", example = "1")
    Long id,

    @Schema(description = "Тип запроса", example = "REASSIGN")
    TicketRequestType type,

    @Schema(description = "Статус запроса", example = "PENDING")
    TicketRequestStatus status,

    @Schema(description = "Обоснование запроса", example = "Требуется профильная экспертиза")
    String reason,

    @Schema(description = "Комментарий руководителя к решению", nullable = true)
    String decisionComment,

    @Schema(description = "Дата и время создания запроса", example = "2026-10-18T10:15:30")
    LocalDateTime createdAt,

    @Schema(description = "Дата и время решения по запросу", nullable = true)
    LocalDateTime decidedAt,

    @Schema(description = "Идентификатор заявки", example = "1")
    Long ticketId,

    @Schema(description = "Специалист, подавший запрос")
    UserSummary requester,

    @Schema(description = "Предлагаемый новый специалист", nullable = true)
    UserSummary targetAssignee,

    @Schema(description = "Руководитель, принявший решение", nullable = true)
    UserSummary decidedBy
) {}
