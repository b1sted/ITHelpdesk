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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.basted.ithelpdesk.core.exception.ApiError;
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
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Заявка успешно создана",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Указанная категория не найдена",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody TicketCreateRequest createRequest
    );

    @Operation(
            summary = "Получить список заявок",
            description = "Возвращает список заявок. Можно отфильтровать по статусу"
    )
    @ApiResponse(responseCode = "200", description = "Список заявок",
            content = @Content(schema = @Schema(implementation = TicketResponse.class)))
    @GetMapping
    ResponseEntity<List<TicketResponse>> getAllTickets(
            @Parameter(description = "Фильтр по статусу заявки", required = false)
            @RequestParam(required = false) TicketStatus status
    );

    @Operation(
            summary = "Получить полную информацию о заявке по идентификатору",
            description = "Возвращает информацию о конкретной заявке с её автором, исполнителем и комментариями"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Полная информация о заявке",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "404", description = "Заявка не найдена",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Заявка назначена и переведена в статус IN_PROGRESS",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "404", description = "Заявка не найдена",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Заявка не находится в статусе NEW",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Статус заявки успешно изменён",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса или недопустимый целевой статус",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Заявка не найдена",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Недопустимый переход состояния или недостаточно прав",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT')")
    ResponseEntity<TicketResponse> changeStatus(
            @Parameter(description = "Уникальный идентификатор заявки", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "ID не может быть меньше нуля") Long id,

            @Valid @RequestBody TicketStatusChangeRequest changeRequest
    );
}