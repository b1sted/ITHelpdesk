package ru.basted.ithelpdesk.ticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель данных для продления крайнего срока заявки")
public record TicketDeadlineExtensionRequest(
    @NotNull(message = "Количество часов для продления обязательно")
    @Min(value = 1, message = "Продлить срок можно минимум на 1 час")
    @Max(value = 720, message = "Продлить срок можно не более чем на 720 часов")
    @Schema(description = "На сколько часов продлить крайний срок", example = "24")
    Integer hours,

    @NotBlank(message = "Причина продления не может быть пустой")
    @Size(max = 1000, message = "Причина продления не может быть длиннее 1000 символов")
    @Schema(description = "Причина продления крайнего срока",
            example = "Ожидается согласование доступа у смежного подразделения")
    String reason
) {}
