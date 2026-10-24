package ru.basted.ithelpdesk.message.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.basted.ithelpdesk.core.exception.ConflictException;
import ru.basted.ithelpdesk.core.exception.NotFoundException;
import ru.basted.ithelpdesk.core.security.CurrentUser;
import ru.basted.ithelpdesk.message.dto.MessageCreateRequest;
import ru.basted.ithelpdesk.message.dto.MessageResponse;
import ru.basted.ithelpdesk.message.entity.Message;
import ru.basted.ithelpdesk.message.mapper.MessageMapper;
import ru.basted.ithelpdesk.message.repository.MessageRepository;
import ru.basted.ithelpdesk.ticket.entity.Ticket;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;
import ru.basted.ithelpdesk.ticket.repository.TicketRepository;
import ru.basted.ithelpdesk.user.dto.UserSummary;
import ru.basted.ithelpdesk.user.entity.User;
import ru.basted.ithelpdesk.user.entity.UserRole;

@Service
public class MessageServiceImpl implements MessageService {
    private final MessageRepository repository;
    private final MessageMapper mapper;
    private final TicketRepository ticketRepository;
    private final CurrentUser currentUser;

    public MessageServiceImpl(
            MessageRepository repository,
            MessageMapper mapper,
            TicketRepository ticketRepository,
            CurrentUser currentUser
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.ticketRepository = ticketRepository;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional
    public MessageResponse addMessage(Long ticketId, MessageCreateRequest createRequest) {
        Ticket ticket = findTicket(ticketId);

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ConflictException("Нельзя добавлять сообщения к закрытой заявке");
        }

        User author = currentUser.get();
        checkAuthorCanPost(ticket, author);

        Message message = mapper.toEntity(createRequest);
        message.setCreatedAt(LocalDateTime.now());
        message.setTicket(ticket);
        message.setAuthor(author);

        if (ticket.getStatus() == TicketStatus.WAITING_USER
                && author.getId().equals(ticket.getCreator().getId())) {
            ticket.setStatus(TicketStatus.ANSWERED);
        }

        return toResponse(repository.save(message));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessagesByTicket(Long ticketId) {
        Ticket ticket = findTicket(ticketId);
        checkViewerCanReadThread(ticket);

        return getMessagesForTicket(ticketId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessagesForTicket(Long ticketId) {
        return repository.findAllByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void checkAuthorCanPost(Ticket ticket, User author) {
        if (author.getRole() == UserRole.ADMIN || author.getRole() == UserRole.MANAGER) {
            throw new ConflictException(
                    "Администратор и руководитель не могут писать сообщения в заявках"
            );
        }

        if (author.getRole() == UserRole.AGENT
                && (ticket.getAssignee() == null
                || !author.getId().equals(ticket.getAssignee().getId()))) {
            throw new ConflictException(
                    "Специалист может писать сообщения только в заявке, взятой им в работу"
            );
        }

        if (author.getRole() == UserRole.EMPLOYEE
                && !author.getId().equals(ticket.getCreator().getId())) {
            throw new ConflictException("Сотрудник может писать сообщения только в своих заявках");
        }
    }

    private void checkViewerCanReadThread(Ticket ticket) {
        User viewer = currentUser.get();

        if (viewer.getRole() == UserRole.EMPLOYEE
                && !viewer.getId().equals(ticket.getCreator().getId())) {
            throw new NotFoundException("Не найдена заявка с ID %d".formatted(ticket.getId()));
        }
    }

    private Ticket findTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException(
                        "Не найдена заявка с ID %d".formatted(ticketId)
                ));
    }

    private MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getText(),
                message.getCreatedAt(),
                message.getTicket().getId(),
                toSummary(message.getAuthor())
        );
    }

    private UserSummary toSummary(User user) {
        return new UserSummary(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getDepartment().getName()
        );
    }
}
