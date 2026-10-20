package ru.basted.ithelpdesk.message.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель данных для добавления нового сообщения в переписку по заявке")
public record MessageCreateRequest(
    @NotBlank(message = "Текст сообщения не может быть пустым")
    @Schema(description = "Текст сообщения", example = "Проблема исправлена, просьба проверить")
    String text
) {}
