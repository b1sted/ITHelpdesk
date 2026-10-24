package ru.basted.ithelpdesk.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Показатели поддержки за один период")
public record PeriodBucketResponse(
    @Schema(description = "Подпись периода", example = "янв.")
    String label,

    @Schema(description = "Создано заявок", example = "12")
    long created,

    @Schema(description = "Решено заявок", example = "10")
    long resolved,

    @Schema(description = "Закрыто заявок", example = "8")
    long closed
) {}
