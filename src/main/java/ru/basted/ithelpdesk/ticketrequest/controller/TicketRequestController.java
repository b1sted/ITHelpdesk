package ru.basted.ithelpdesk.ticketrequest.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.basted.ithelpdesk.ticketrequest.dto.TicketRequestCreateRequest;
import ru.basted.ithelpdesk.ticketrequest.dto.TicketRequestDecisionRequest;
import ru.basted.ithelpdesk.ticketrequest.dto.TicketRequestResponse;
import ru.basted.ithelpdesk.ticketrequest.entity.TicketRequestStatus;
import ru.basted.ithelpdesk.ticketrequest.service.TicketRequestService;

@RestController
@RequestMapping("/api/v1")
public class TicketRequestController implements TicketRequestAPI {
    private final TicketRequestService service;

    public TicketRequestController(TicketRequestService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<TicketRequestResponse> createRequest(
            Long ticketId,
            TicketRequestCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createRequest(ticketId, request));
    }

    @Override
    public ResponseEntity<List<TicketRequestResponse>> getTicketRequests(Long ticketId) {
        return ResponseEntity.ok(service.getTicketRequests(ticketId));
    }

    @Override
    public ResponseEntity<List<TicketRequestResponse>> getRequests(TicketRequestStatus status) {
        return ResponseEntity.ok(service.getRequests(status));
    }

    @Override
    public ResponseEntity<TicketRequestResponse> decide(
            Long id,
            TicketRequestDecisionRequest decision
    ) {
        return ResponseEntity.ok(service.decide(id, decision));
    }
}
