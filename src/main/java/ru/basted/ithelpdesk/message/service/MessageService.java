package ru.basted.ithelpdesk.message.service;

import java.util.List;

import ru.basted.ithelpdesk.message.dto.MessageCreateRequest;
import ru.basted.ithelpdesk.message.dto.MessageResponse;

public interface MessageService {
    MessageResponse addMessage(Long ticketId, MessageCreateRequest createRequest);

    List<MessageResponse> getMessagesByTicket(Long ticketId);
}
