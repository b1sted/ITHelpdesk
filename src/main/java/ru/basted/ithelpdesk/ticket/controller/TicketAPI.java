package ru.basted.ithelpdesk.ticket.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.MediaType;
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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
    String TICKET_JSON = """
            {
              "id": 1,
              "title": "Не работает корпоративная почта",
              "description": "С утра не могу войти в почтовый клиент",
              "status": "IN_PROGRESS",
              "createdAt": "2026-10-18T10:15:30",
              "creator": {
                "id": 1,
                "fullName": "Иванов Иван Иванович",
                "email": "ivanov.ii@company.ru",
                "departmentName": "Департамент тестирования программных продуктов"
              },
              "assignee": {
                "id": 2,
                "fullName": "Петров Пётр Петрович",
                "email": "petrov.pp@company.ru",
                "departmentName": "Департамент разработки"
              },
              "categoryName": "Программное обеспечение",
              "messages": [
                {
                  "id": 1,
                  "text": "Принял заявку в работу, проверяю почтовый сервер",
                  "createdAt": "2026-10-18T10:20:00",
                  "ticketId": 1,
                  "author": {
                    "id": 2,
                    "fullName": "Петров Пётр Петрович",
                    "email": "petrov.pp@company.ru",
                    "departmentName": "Департамент разработки"
                  }
                }
              ]
            }""";
    String TICKET_LIST_JSON = "[" + TICKET_JSON + "]";
    String ERROR_404_GENERIC_JSON = "{\"message\":\"Ресурс не найден\"}";
    String ERROR_404_TICKET_JSON = "{\"message\":\"Не найдена заявка с ID 99\"}";
    String ERROR_404_CATEGORY_JSON = "{\"message\":\"Не найдена категория с ID 99\"}";
    String ERROR_400_JSON = "{\"message\":\"Заголовок заявки не может быть пустым\"}";
    String ERROR_409_JSON = "{\"message\":\"Нельзя взять в работу заявку в статусе RESOLVED\"}";
    String ERROR_409_STATUS_JSON = "{\"message\":\"Закрыть заявку можно только из статуса RESOLVED\"}";

    @Operation(
            summary = "Создать новую заявку",
            description = "Регистрация новой заявки. Доступно только сотрудникам"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Заявка успешно создана",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TicketResponse.class),
                            examples = @ExampleObject(value = TICKET_JSON))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_400_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Категория не найдена или ресурс скрыт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Не найдена категория", value = ERROR_404_CATEGORY_JSON),
                                    @ExampleObject(name = "Ресурс скрыт", value = ERROR_404_GENERIC_JSON)
                            }))
    })
    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody TicketCreateRequest createRequest
    );

    @Operation(
            summary = "Получить список заявок",
            description = "Возвращает список заявок. Сотрудник видит только свои заявки, "
                    + "специалист — назначенные ему и новые, руководитель и администратор — все. "
                    + "Можно отфильтровать по статусу"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список заявок",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = TicketResponse.class)),
                            examples = @ExampleObject(value = TICKET_LIST_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Ресурс не найден или требуется аутентификация",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_404_GENERIC_JSON)))
    })
    @GetMapping
    ResponseEntity<List<TicketResponse>> getAllTickets(
            @Parameter(description = "Фильтр по статусу заявки")
            @RequestParam(required = false) TicketStatus status
    );

    @Operation(
            summary = "Получить полную информацию о заявке по идентификатору",
            description = "Возвращает информацию о конкретной заявке с её автором, исполнителем и перепиской. "
                    + "Сотрудник может читать только свои заявки, специалист, руководитель и администратор — любые"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Полная информация о заявке",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TicketResponse.class),
                            examples = @ExampleObject(value = TICKET_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Заявка не найдена или ресурс скрыт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Не найдена заявка", value = ERROR_404_TICKET_JSON),
                                    @ExampleObject(name = "Ресурс скрыт", value = ERROR_404_GENERIC_JSON)
                            }))
    })
    @GetMapping("/{id}")
    ResponseEntity<TicketResponse> getTicketById(
            @Parameter(description = "Уникальный идентификатор заявки", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "Идентификатор должен быть положительным") Long id
    );

    @Operation(
            summary = "Взять заявку в работу",
            description = "Назначение заявки на текущего специалиста. Доступно только специалистам"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Заявка назначена и переведена в статус IN_PROGRESS",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TicketResponse.class),
                            examples = @ExampleObject(value = TICKET_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Заявка не найдена или ресурс скрыт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Не найдена заявка", value = ERROR_404_TICKET_JSON),
                                    @ExampleObject(name = "Ресурс скрыт", value = ERROR_404_GENERIC_JSON)
                            })),
            @ApiResponse(responseCode = "409", description = "Заявка не находится в статусе NEW",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_409_JSON)))
    })
    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('AGENT')")
    ResponseEntity<TicketResponse> assignTicket(
            @Parameter(description = "Уникальный идентификатор заявки", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "Идентификатор должен быть положительным") Long id
    );

    @Operation(
            summary = "Изменить статус заявки",
            description = "Перевод заявки в новый статус: приостановка, решение, закрытие или возврат на доработку"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Статус заявки успешно изменён",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TicketResponse.class),
                            examples = @ExampleObject(value = TICKET_JSON))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса или недопустимый целевой статус",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_400_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Заявка не найдена или ресурс скрыт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Не найдена заявка", value = ERROR_404_TICKET_JSON),
                                    @ExampleObject(name = "Ресурс скрыт", value = ERROR_404_GENERIC_JSON)
                            })),
            @ApiResponse(responseCode = "409", description = "Недопустимый переход состояния или недостаточно прав",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_409_STATUS_JSON)))
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT')")
    ResponseEntity<TicketResponse> changeStatus(
            @Parameter(description = "Уникальный идентификатор заявки", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "Идентификатор должен быть положительным") Long id,

            @Valid @RequestBody TicketStatusChangeRequest changeRequest
    );
}