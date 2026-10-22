package ru.basted.ithelpdesk.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Статистика по специалисту поддержки")
public record AgentStatisticsResponse(
    @Schema(description = "Уникальный идентификатор специалиста", example = "2")
    Long agentId,

    @Schema(description = "Полное имя специалиста", example = "Петров Пётр Петрович")
    String fullName,

    @Schema(description = "Электронная почта специалиста", example = "petrov.pp@company.ru")
    String email,

    @Schema(description = "Название департамента", example = "IT Service Desk")
    String departmentName,

    @Schema(description = "Предел одновременно взятых в работу заявок", example = "10")
    Integer ticketLimit,

    @Schema(description = "Предельное время выполнения назначенной заявки в часах", example = "24")
    Integer timeLimitHours,

    @Schema(description = "Всего назначенных заявок", example = "42")
    long totalAssigned,

    @Schema(description = "Активные заявки в работе", example = "5")
    long activeTickets,

    @Schema(description = "Решённые заявки", example = "20")
    long resolvedTickets,

    @Schema(description = "Закрытые заявки", example = "15")
    long closedTickets,

    @Schema(description = "Просроченные активные заявки", example = "2")
    long overdueTickets
) {}
