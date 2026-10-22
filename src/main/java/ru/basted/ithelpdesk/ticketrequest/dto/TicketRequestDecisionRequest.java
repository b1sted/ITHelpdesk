package ru.basted.ithelpdesk.ticketrequest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.basted.ithelpdesk.ticketrequest.entity.TicketRequestStatus;

@Schema(description = "Модель данных для решения руководителя по запросу специалиста")
public record TicketRequestDecisionRequest(
    @NotNull(message = "Решение по запросу обязательно")
    @Schema(description = "Решение по запросу: APPROVED или REJECTED", example = "APPROVED")
    TicketRequestStatus status,

    @Size(max = 1000, message = "Комментарий не может быть длиннее 1000 символов")
    @Schema(description = "Комментарий руководителя к решению", example = "Согласовано")
    String comment
) {}
