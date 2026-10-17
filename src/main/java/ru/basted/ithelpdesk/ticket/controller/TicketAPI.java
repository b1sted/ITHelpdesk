package ru.basted.ithelpdesk.ticket.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.basted.ithelpdesk.ticket.dto.TicketCreateRequest;
import ru.basted.ithelpdesk.ticket.dto.TicketResponse;
import ru.basted.ithelpdesk.ticket.dto.TicketStatusChangeRequest;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;

@Tag(name = "Заявки", description = "Управление заявками и инцидентами")
public interface TicketAPI {
    @Operation(
            summary = "Создать новую заявку",
            description = "Регистрация новой заявки. Доступно только сотрудникам"
    )
    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody TicketCreateRequest createRequest
    );

    @Operation(
            summary = "Получить список заявок",
            description = "Возвращает список заявок. Можно отфильтровать по статусу"
    )
    @GetMapping
    ResponseEntity<List<TicketResponse>> getAllTickets(
            @Parameter(description = "Фильтр по статусу заявки", required = false)
            @RequestParam(required = false) TicketStatus status
    );

    @Operation(
            summary = "Получить информацию о заявке по идентификатору",
            description = "Возвращает информацию о конкретной заявке с её автором и исполнителем"
    )
    @GetMapping("/{id}")
    ResponseEntity<TicketResponse> getTicketById(
            @Parameter(description = "Уникальный идентификатор заявки", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "ID не может быть меньше нуля") Long id
    );

    @Operation(
            summary = "Взять заявку в работу",
            description = "Назначение заявки на текущего специалиста. Доступно только специалистам"
    )
    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('AGENT')")
    ResponseEntity<TicketResponse> assignTicket(
            @Parameter(description = "Уникальный идентификатор заявки", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "ID не может быть меньше нуля") Long id
    );

    @Operation(
            summary = "Изменить статус заявки",
            description = "Перевод заявки в новый статус: приостановка, решение, закрытие или возврат на доработку"
    )
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT')")
    ResponseEntity<TicketResponse> changeStatus(
            @Parameter(description = "Уникальный идентификатор заявки", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "ID не может быть меньше нуля") Long id,

            @Valid @RequestBody TicketStatusChangeRequest changeRequest
    );
}