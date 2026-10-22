package ru.basted.ithelpdesk.ticketrequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.basted.ithelpdesk.ticketrequest.entity.TicketRequestType;

@Schema(description = "Модель данных для запроса отказа или переназначения заявки")
public record TicketRequestCreateRequest(
    @NotNull(message = "Тип запроса обязателен")
    @Schema(description = "Тип запроса: RELEASE — отказ от заявки, REASSIGN — переназначение", example = "RELEASE")
    TicketRequestType type,

    @NotBlank(message = "Обоснование запроса не может быть пустым")
    @Size(max = 1000, message = "Обоснование не может быть длиннее 1000 символов")
    @Schema(description = "Обоснование запроса для руководителя",
            example = "Требуется профильная экспертиза другого специалиста")
    String reason,

    @Schema(description = "Идентификатор специалиста для переназначения (обязателен для REASSIGN)",
            example = "3")
    Long targetAssigneeId
) {}
