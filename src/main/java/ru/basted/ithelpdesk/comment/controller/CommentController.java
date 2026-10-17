package ru.basted.ithelpdesk.comment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.basted.ithelpdesk.comment.dto.CommentCreateRequest;
import ru.basted.ithelpdesk.comment.dto.CommentResponse;
import ru.basted.ithelpdesk.comment.service.CommentService;

@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/comments")
public class CommentController implements CommentAPI {
    private final CommentService service;

    public CommentController(CommentService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<CommentResponse> addComment(Long ticketId, CommentCreateRequest createRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.addComment(ticketId, createRequest));
    }

    @Override
    public ResponseEntity<List<CommentResponse>> getCommentsByTicket(Long ticketId) {
        return ResponseEntity.ok(service.getCommentsByTicket(ticketId));
    }
}