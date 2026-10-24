package ru.basted.ithelpdesk.settings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Настройки работы службы поддержки")
public record SupportSettingsUpdateRequest(
    @NotNull(message = "Срок автозакрытия обязателен")
    @Min(value = 0, message = "Срок автозакрытия не может быть отрицательным")
    @Max(value = 8760, message = "Срок автозакрытия не может превышать 8760 часов")
    @Schema(description = "Через сколько часов автоматически закрывать решённые заявки. "
            + "0 — автозакрытие отключено", example = "72")
    Integer autoCloseResolvedHours
) {}
