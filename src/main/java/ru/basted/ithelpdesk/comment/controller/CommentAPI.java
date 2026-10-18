package ru.basted.ithelpdesk.comment.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.basted.ithelpdesk.comment.dto.CommentCreateRequest;
import ru.basted.ithelpdesk.comment.dto.CommentResponse;
import ru.basted.ithelpdesk.core.exception.ApiError;

@Tag(name = "Комментарии", description = "Обсуждение заявок в комментариях")
public interface CommentAPI {
    @Operation(
            summary = "Добавить комментарий к заявке",
            description = "Добавление нового сообщения в обсуждение заявки"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Комментарий успешно добавлен",
                    content = @Content(schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Заявка не найдена",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Нельзя комментировать закрытую заявку",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
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
                    content = @Content(schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Заявка не найдена",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    ResponseEntity<List<CommentResponse>> getCommentsByTicket(
            @Parameter(description = "Уникальный идентификатор заявки", required = true, example = "1")
            @PathVariable("ticketId")
            @Min(value = 1, message = "ID не может быть меньше нуля") Long ticketId
    );
}