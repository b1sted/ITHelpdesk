package ru.basted.ithelpdesk.message.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.basted.ithelpdesk.user.dto.UserSummary;

@Schema(description = "Модель данных, возвращаемая при получении информации о сообщении")
public record MessageResponse(
    @Schema(description = "Уникальный идентификатор сообщения", example = "1")
    Long id,

    @Schema(description = "Текст сообщения", example = "Проблема исправлена, просьба проверить")
    String text,

    @Schema(description = "Дата и время создания сообщения", example = "2026-10-18T10:15:30")
    LocalDateTime createdAt,

    @Schema(description = "Идентификатор заявки, к которой относится сообщение", example = "1")
    Long ticketId,

    @Schema(description = "Автор сообщения")
    UserSummary author
) {}
