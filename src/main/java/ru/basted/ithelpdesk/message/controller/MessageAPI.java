package ru.basted.ithelpdesk.message.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
import ru.basted.ithelpdesk.message.dto.MessageCreateRequest;
import ru.basted.ithelpdesk.message.dto.MessageResponse;

@Tag(name = "Сообщения", description = "Переписка по заявке в ходе решения проблемы")
public interface MessageAPI {
    String MESSAGE_JSON = """
            {
              "id": 1,
              "text": "Проблема исправлена, просьба проверить",
              "createdAt": "2026-10-18T10:15:30",
              "ticketId": 1,
              "author": {
                "id": 2,
                "fullName": "Петров Пётр Петрович",
                "email": "petrov.pp@company.ru",
                "departmentName": "Департамент разработки"
              }
            }""";
    String MESSAGE_LIST_JSON = "[" + MESSAGE_JSON + "]";
    String ERROR_404_GENERIC_JSON = "{\"message\":\"Ресурс не найден\"}";
    String ERROR_404_TICKET_JSON = "{\"message\":\"Не найдена заявка с ID 99\"}";
    String ERROR_400_JSON = "{\"message\":\"Текст сообщения не может быть пустым\"}";
    String ERROR_409_JSON = "{\"message\":\"Нельзя добавлять сообщения к закрытой заявке\"}";
    String ERROR_409_AGENT_JSON =
            "{\"message\":\"Специалист может писать сообщения только в заявке, взятой им в работу\"}";
    String ERROR_409_EMPLOYEE_JSON =
            "{\"message\":\"Сотрудник может писать сообщения только в своих заявках\"}";

    @Operation(
            summary = "Добавить сообщение в переписку по заявке",
            description = "Добавление нового сообщения в переписку по заявке в ходе решения проблемы. "
                    + "Доступно автору заявки и назначенному специалисту"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Сообщение успешно добавлено",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = MESSAGE_JSON))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса",
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
            @ApiResponse(responseCode = "409",
                    description = "Закрытая заявка или специалист не взял заявку в работу",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Закрытая заявка", value = ERROR_409_JSON),
                                    @ExampleObject(name = "Заявка не взята в работу", value = ERROR_409_AGENT_JSON),
                                    @ExampleObject(name = "Чужая заявка", value = ERROR_409_EMPLOYEE_JSON)
                            }))
    })
    @PostMapping("/{ticketId}/messages")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT')")
    ResponseEntity<MessageResponse> addMessage(
            @Parameter(description = "Уникальный идентификатор заявки", required = true, example = "1")
            @PathVariable("ticketId")
            @Min(value = 1, message = "Идентификатор должен быть положительным") Long ticketId,

            @Valid @RequestBody MessageCreateRequest createRequest
    );

    @Operation(
            summary = "Получить переписку по заявке",
            description = "Возвращает хронологический список сообщений по заявке. "
                    + "Сотрудник может читать переписку только по своим заявкам"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список сообщений",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = MessageResponse.class)),
                            examples = @ExampleObject(value = MESSAGE_LIST_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Заявка не найдена или ресурс скрыт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Не найдена заявка", value = ERROR_404_TICKET_JSON),
                                    @ExampleObject(name = "Ресурс скрыт", value = ERROR_404_GENERIC_JSON)
                            }))
    })
    @GetMapping("/{ticketId}/messages")
    ResponseEntity<List<MessageResponse>> getMessagesByTicket(
            @Parameter(description = "Уникальный идентификатор заявки", required = true, example = "1")
            @PathVariable("ticketId")
            @Min(value = 1, message = "Идентификатор должен быть положительным") Long ticketId
    );
}
