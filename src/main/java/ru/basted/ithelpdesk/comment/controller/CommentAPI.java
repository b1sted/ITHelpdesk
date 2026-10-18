package ru.basted.ithelpdesk.comment.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.basted.ithelpdesk.comment.dto.CommentCreateRequest;
import ru.basted.ithelpdesk.comment.dto.CommentResponse;
import ru.basted.ithelpdesk.core.exception.ApiError;

@Tag(name = "Комментарии", description = "Обсуждение заявок в комментариях")
public interface CommentAPI {
    String COMMENT_JSON = """
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
    String COMMENT_LIST_JSON = "[" + COMMENT_JSON + "]";
    String ERROR_401_JSON = "{\"message\":\"Требуется аутентификация\"}";
    String ERROR_404_JSON = "{\"message\":\"Не найдена заявка с ID 99\"}";
    String ERROR_400_JSON = "{\"message\":\"Текст комментария не может быть пустым\"}";
    String ERROR_409_JSON = "{\"message\":\"Нельзя добавлять комментарии к закрытой заявке\"}";

    @Operation(
            summary = "Добавить комментарий к заявке",
            description = "Добавление нового сообщения в обсуждение заявки"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Комментарий успешно добавлен",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommentResponse.class),
                            examples = @ExampleObject(value = COMMENT_JSON))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_400_JSON))),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_401_JSON))),
            @ApiResponse(responseCode = "404", description = "Заявка не найдена",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_404_JSON))),
            @ApiResponse(responseCode = "409", description = "Нельзя комментировать закрытую заявку",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_409_JSON)))
    })
    @PostMapping
    ResponseEntity<CommentResponse> addComment(
            @Parameter(description = "Уникальный идентификатор заявки", required = true, example = "1")
            @PathVariable("ticketId")
            @Min(value = 1, message = "ID не может быть меньше нуля") Long ticketId,

            @Valid @RequestBody CommentCreateRequest createRequest
    );

    @Operation(
            summary = "Получить комментарии к заявке",
            description = "Возвращает хронологический список комментариев заявки"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список комментариев",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommentResponse.class),
                            examples = @ExampleObject(value = COMMENT_LIST_JSON))),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_401_JSON))),
            @ApiResponse(responseCode = "404", description = "Заявка не найдена",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_404_JSON)))
    })
    @GetMapping
    ResponseEntity<List<CommentResponse>> getCommentsByTicket(
            @Parameter(description = "Уникальный идентификатор заявки", required = true, example = "1")
            @PathVariable("ticketId")
            @Min(value = 1, message = "ID не может быть меньше нуля") Long ticketId
    );
}