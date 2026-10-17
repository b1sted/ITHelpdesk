package ru.basted.ithelpdesk.ticket.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.basted.ithelpdesk.comment.dto.CommentResponse;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;
import ru.basted.ithelpdesk.user.dto.UserSummary;

@Schema(description = "Модель данных, возвращаемая при создании или получении информации о заявке")
public record TicketResponse(
    @Schema(description = "Уникальный идентификатор заявки", example = "1")
    Long id,

    @Schema(description = "Заголовок заявки", example = "Не работает корпоративная почта")
    String title,

    @Schema(description = "Описание проблемы", example = "С утра не могу войти в почтовый клиент")
    String description,

    @Schema(description = "Статус заявки", example = "NEW")
    TicketStatus status,

    @Schema(description = "Дата и время создания заявки", example = "2026-10-18T10:15:30")
    LocalDateTime createdAt,

    @Schema(description = "Автор заявки")
    UserSummary creator,

    @Schema(description = "Исполнитель заявки")
    UserSummary assignee,

    @Schema(description = "Название категории инцидента", example = "Оборудование")
    String categoryName,

    @Schema(description = "Комментарии к заявке, перечисленные в хронологическом порядке")
    List<CommentResponse> comments
) {}