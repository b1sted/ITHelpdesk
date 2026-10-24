package ru.basted.ithelpdesk.statistics.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Статистика поддержки по месяцам или годам")
public record PeriodStatisticsResponse(
    @Schema(description = "Гранулярность статистики", example = "MONTH")
    StatisticsGranularity granularity,

    @Schema(description = "Показатели по периодам")
    List<PeriodBucketResponse> buckets,

    @Schema(description = "Всего создано заявок за выбранный диапазон", example = "120")
    long totalCreated,

    @Schema(description = "Всего решено заявок за выбранный диапазон", example = "95")
    long totalResolved,

    @Schema(description = "Всего закрыто заявок за выбранный диапазон", example = "80")
    long totalClosed,

    @Schema(description = "Среднее время решения заявки в часах", example = "18.5")
    double averageResolutionHours
) {}
