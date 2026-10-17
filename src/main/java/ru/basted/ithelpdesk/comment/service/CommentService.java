package ru.basted.ithelpdesk.comment.service;

import java.util.List;

import ru.basted.ithelpdesk.comment.dto.CommentCreateRequest;
import ru.basted.ithelpdesk.comment.dto.CommentResponse;

public interface CommentService {
    CommentResponse addComment(Long ticketId, CommentCreateRequest createRequest);

    List<CommentResponse> getCommentsByTicket(Long ticketId);
}