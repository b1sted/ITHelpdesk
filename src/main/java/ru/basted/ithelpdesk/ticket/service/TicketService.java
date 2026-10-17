package ru.basted.ithelpdesk.ticket.service;

import java.util.List;

import ru.basted.ithelpdesk.ticket.dto.TicketCreateRequest;
import ru.basted.ithelpdesk.ticket.dto.TicketResponse;
import ru.basted.ithelpdesk.ticket.dto.TicketStatusChangeRequest;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;

public interface TicketService {
    TicketResponse createTicket(TicketCreateRequest createRequest);

    List<TicketResponse> getAllTickets(TicketStatus status);

    TicketResponse getTicketDetails(Long id);

    TicketResponse assignTicket(Long id);

    TicketResponse changeStatus(Long id, TicketStatusChangeRequest changeRequest);
}