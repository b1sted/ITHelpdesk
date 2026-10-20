package ru.basted.ithelpdesk.message.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.basted.ithelpdesk.message.dto.MessageCreateRequest;
import ru.basted.ithelpdesk.message.dto.MessageResponse;
import ru.basted.ithelpdesk.message.service.MessageService;

@RestController
@RequestMapping("/api/v1/tickets")
public class MessageController implements MessageAPI {
    private final MessageService service;

    public MessageController(MessageService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<MessageResponse> addMessage(Long ticketId, MessageCreateRequest createRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.addMessage(ticketId, createRequest));
    }

    @Override
    public ResponseEntity<List<MessageResponse>> getMessagesByTicket(Long ticketId) {
        return ResponseEntity.ok(service.getMessagesByTicket(ticketId));
    }
}
