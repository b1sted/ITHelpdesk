package ru.basted.ithelpdesk.ticketrequest.service;

import java.util.List;

import ru.basted.ithelpdesk.ticketrequest.dto.TicketRequestCreateRequest;
import ru.basted.ithelpdesk.ticketrequest.dto.TicketRequestDecisionRequest;
import ru.basted.ithelpdesk.ticketrequest.dto.TicketRequestResponse;
import ru.basted.ithelpdesk.ticketrequest.entity.TicketRequestStatus;

public interface TicketRequestService {
    TicketRequestResponse createRequest(Long ticketId, TicketRequestCreateRequest request);

    List<TicketRequestResponse> getTicketRequests(Long ticketId);

    List<TicketRequestResponse> getRequests(TicketRequestStatus status);

    TicketRequestResponse decide(Long requestId, TicketRequestDecisionRequest decision);
}
