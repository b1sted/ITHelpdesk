package ru.basted.ithelpdesk.ticket.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.basted.ithelpdesk.ticket.dto.TicketCreateRequest;
import ru.basted.ithelpdesk.ticket.dto.TicketResponse;
import ru.basted.ithelpdesk.ticket.dto.TicketStatusChangeRequest;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;
import ru.basted.ithelpdesk.ticket.service.TicketService;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController implements TicketAPI {
    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<TicketResponse> createTicket(TicketCreateRequest createRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createTicket(createRequest));
    }

    @Override
    public ResponseEntity<List<TicketResponse>> getAllTickets(TicketStatus status) {
        return ResponseEntity.ok(service.getAllTickets(status));
    }

    @Override
    public ResponseEntity<TicketResponse> getTicketById(Long id) {
        return ResponseEntity.ok(service.getTicketById(id));
    }

    @Override
    public ResponseEntity<TicketResponse> assignTicket(Long id) {
        return ResponseEntity.ok(service.assignTicket(id));
    }

    @Override
    public ResponseEntity<TicketResponse> changeStatus(Long id, TicketStatusChangeRequest changeRequest) {
        return ResponseEntity.ok(service.changeStatus(id, changeRequest));
    }
}