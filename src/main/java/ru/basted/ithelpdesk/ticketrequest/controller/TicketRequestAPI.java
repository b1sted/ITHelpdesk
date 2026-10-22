package ru.basted.ithelpdesk.ticketrequest.controller;

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
import ru.basted.ithelpdesk.ticketrequest.dto.TicketRequestCreateRequest;
import ru.basted.ithelpdesk.ticketrequest.dto.TicketRequestDecisionRequest;
import ru.basted.ithelpdesk.ticketrequest.dto.TicketRequestResponse;
import ru.basted.ithelpdesk.ticketrequest.entity.TicketRequestStatus;

@Tag(name = "Запросы по заявкам",
        description = "Отказ от заявки и переназначение на другого специалиста с решением руководителя")
public interface TicketRequestAPI {
    String REQUEST_JSON = """
            {
              "id": 1,
              "type": "REASSIGN",
              "status": "PENDING",
              "reason": "Требуется профильная экспертиза другого специалиста",
              "decisionComment": null,
              "createdAt": "2026-10-18T10:15:30",
              "decidedAt": null,
              "ticketId": 1,
              "requester": {
                "id": 2,
                "fullName": "Петров Пётр Петрович",
                "email": "petrov.pp@company.ru",
                "departmentName": "IT Service Desk"
              },
              "targetAssignee": {
                "id": 3,
                "fullName": "Сидоров Сидор Сидорович",
                "email": "sidorov.ss@company.ru",
                "departmentName": "IT Service Desk"
              },
              "decidedBy": null
            }""";
    String REQUEST_LIST_JSON = "[" + REQUEST_JSON + "]";
    String ERROR_404_GENERIC_JSON = "{\"message\":\"Ресурс не найден\"}";
    String ERROR_404_TICKET_JSON = "{\"message\":\"Не найдена заявка с ID 99\"}";
    String ERROR_409_JSON = "{\"message\":\"По заявке уже есть запрос, ожидающий решения руководителя\"}";
    String ERROR_409_DECISION_JSON = "{\"message\":\"Запрос уже рассмотрен руководителем\"}";

    @Operation(
            summary = "Создать запрос отказа или переназначения",
            description = "Назначенный специалист создаёт запрос на отказ от заявки или её переназначение "
                    + "другому специалисту с обязательным обоснованием. Решение принимает руководитель"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Запрос создан и ожидает решения",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TicketRequestResponse.class),
                            examples = @ExampleObject(value = REQUEST_JSON))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = "{\"message\":\"Обоснование запроса не может быть пустым\"}"))) ,
            @ApiResponse(responseCode = "404",
                    description = "Заявка или специалист не найдены, ресурс скрыт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Не найдена заявка", value = ERROR_404_TICKET_JSON),
                                    @ExampleObject(name = "Ресурс скрыт", value = ERROR_404_GENERIC_JSON)
                            })),
            @ApiResponse(responseCode = "409", description = "Запрос уже существует или заявка недоступна",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_409_JSON)))
    })
    @PostMapping("/tickets/{ticketId}/requests")
    @PreAuthorize("hasRole('AGENT')")
    ResponseEntity<TicketRequestResponse> createRequest(
            @Parameter(description = "Уникальный идентификатор заявки", required = true, example = "1")
            @PathVariable("ticketId")
            @Min(value = 1, message = "Идентификатор должен быть положительным") Long ticketId,

            @Valid @RequestBody TicketRequestCreateRequest request
    );

    @Operation(
            summary = "Получить запросы по заявке",
            description = "Возвращает историю запросов отказа и переназначения по конкретной заявке"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список запросов по заявке",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = TicketRequestResponse.class)),
                            examples = @ExampleObject(value = REQUEST_LIST_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Заявка не найдена или ресурс скрыт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_404_TICKET_JSON)))
    })
    @GetMapping("/tickets/{ticketId}/requests")
    @PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
    ResponseEntity<List<TicketRequestResponse>> getTicketRequests(
            @Parameter(description = "Уникальный идентификатор заявки", required = true, example = "1")
            @PathVariable("ticketId")
            @Min(value = 1, message = "Идентификатор должен быть положительным") Long ticketId
    );

    @Operation(
            summary = "Получить запросы на рассмотрение",
            description = "Возвращает запросы специалистов. Доступно руководителю и администратору"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список запросов",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = TicketRequestResponse.class)),
                            examples = @ExampleObject(value = REQUEST_LIST_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Ресурс не найден или недостаточно прав",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_404_GENERIC_JSON)))
    })
    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    ResponseEntity<List<TicketRequestResponse>> getRequests(
            @Parameter(description = "Фильтр по статусу запроса")
            @RequestParam(required = false) TicketRequestStatus status
    );

    @Operation(
            summary = "Принять решение по запросу",
            description = "Руководитель или администратор одобряет либо отклоняет запрос. "
                    + "При одобрении заявка освобождается или переназначается"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Решение по запросу сохранено",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TicketRequestResponse.class),
                            examples = @ExampleObject(value = REQUEST_JSON))),
            @ApiResponse(responseCode = "400", description = "Некорректное решение",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Решение по запросу должно быть APPROVED или REJECTED\"}"))),
            @ApiResponse(responseCode = "404",
                    description = "Запрос не найден или ресурс скрыт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_404_GENERIC_JSON))),
            @ApiResponse(responseCode = "409", description = "Запрос уже рассмотрен или заявка изменилась",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_409_DECISION_JSON)))
    })
    @PatchMapping("/requests/{id}/decision")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    ResponseEntity<TicketRequestResponse> decide(
            @Parameter(description = "Уникальный идентификатор запроса", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "Идентификатор должен быть положительным") Long id,

            @Valid @RequestBody TicketRequestDecisionRequest decision
    );
}
