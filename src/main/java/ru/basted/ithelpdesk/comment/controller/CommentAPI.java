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
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.basted.ithelpdesk.comment.dto.CommentCreateRequest;
import ru.basted.ithelpdesk.comment.dto.CommentResponse;

@Tag(name = "Комментарии", description = "Обсуждение заявок в комментариях")
public interface CommentAPI {
    @Operation(
            summary = "Добавить комментарий к заявке",
            description = "Добавление нового сообщения в обсуждение заявки"
    )
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
    @GetMapping
    ResponseEntity<List<CommentResponse>> getCommentsByTicket(
            @Parameter(description = "Уникальный идентификатор заявки", required = true, example = "1")
            @PathVariable("ticketId")
            @Min(value = 1, message = "ID не может быть меньше нуля") Long ticketId
    );
}