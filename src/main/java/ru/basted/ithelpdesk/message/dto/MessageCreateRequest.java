package ru.basted.ithelpdesk.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель данных для добавления нового сообщения в переписку по заявке")
public record MessageCreateRequest(
    @NotBlank(message = "Текст сообщения не может быть пустым")
    @Size(max = 10000, message = "Текст сообщения не может быть длиннее 10000 символов")
    @Schema(description = "Текст сообщения", example = "Проблема исправлена, просьба проверить")
    String text
) {}
