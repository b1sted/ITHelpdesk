package ru.basted.ithelpdesk.comment.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.basted.ithelpdesk.user.dto.UserSummary;

@Schema(description = "Модель данных, возвращаемая при получении информации о комментарии")
public record CommentResponse(
    @Schema(description = "Уникальный идентификатор комментария", example = "1")
    Long id,

    @Schema(description = "Текст комментария", example = "Проблема исправлена, просьба проверить")
    String text,

    @Schema(description = "Дата и время создания комментария", example = "2026-10-18T10:15:30")
    LocalDateTime createdAt,

    @Schema(description = "Идентификатор заявки, к которой относится комментарий", example = "1")
    Long ticketId,

    @Schema(description = "Автор комментария")
    UserSummary author
) {}