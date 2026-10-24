package ru.basted.ithelpdesk.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Настройки работы службы поддержки")
public record SupportSettingsResponse(
    @Schema(description = "Через сколько часов автоматически закрывать решённые заявки. "
            + "0 — автозакрытие отключено", example = "72")
    int autoCloseResolvedHours
) {}
