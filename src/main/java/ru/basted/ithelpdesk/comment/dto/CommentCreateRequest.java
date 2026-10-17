package ru.basted.ithelpdesk.comment.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель данных для добавления нового комментария к заявке")
public record CommentCreateRequest(
    @NotBlank(message = "Текст комментария не может быть пустым")
    @Schema(description = "Текст комментария", example = "Проблема исправлена, просьба проверить")
    String text
) {}