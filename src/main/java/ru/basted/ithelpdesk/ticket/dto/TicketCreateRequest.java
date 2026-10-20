package ru.basted.ithelpdesk.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель данных для создания новой заявки")
public record TicketCreateRequest(
    @NotBlank(message = "Заголовок заявки не может быть пустым")
    @Size(max = 200, message = "Заголовок заявки не может быть длиннее 200 символов")
    @Schema(description = "Заголовок заявки", example = "Не работает корпоративная почта")
    String title,

    @NotBlank(message = "Описание заявки не может быть пустым")
    @Size(max = 10000, message = "Описание заявки не может быть длиннее 10000 символов")
    @Schema(description = "Описание проблемы", example = "С утра не могу войти в почтовый клиент")
    String description,

    @NotNull(message = "Идентификатор категории обязателен")
    @Schema(description = "Идентификатор категории инцидента", example = "1")
    Long categoryId
) {}