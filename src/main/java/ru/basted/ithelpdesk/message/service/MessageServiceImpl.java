package ru.basted.ithelpdesk.message.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.basted.ithelpdesk.core.exception.ConflictException;
import ru.basted.ithelpdesk.core.exception.NotFoundException;
import ru.basted.ithelpdesk.core.security.CurrentUser;
import ru.basted.ithelpdesk.department.entity.Department;
import ru.basted.ithelpdesk.department.repository.DepartmentRepository;
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
import ru.basted.ithelpdesk.user.repository.UserRepository;

@Service
public class MessageServiceImpl implements MessageService {
    private final MessageRepository repository;
    private final MessageMapper mapper;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CurrentUser currentUser;

    public MessageServiceImpl(
            MessageRepository repository,
            MessageMapper mapper,
            TicketRepository ticketRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            CurrentUser currentUser
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional
    public MessageResponse addMessage(Long ticketId, MessageCreateRequest createRequest) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Не найдена заявка с ID " + ticketId));

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ConflictException("Нельзя добавлять сообщения к закрытой заявке");
        }

        User author = currentUser.get();

        if (author.getRole() == UserRole.AGENT
                && !author.getId().equals(ticket.getAssigneeId())) {
            throw new ConflictException(
                    "Специалист может писать сообщения только в заявке, взятой им в работу"
            );
        }

        Message message = mapper.toEntity(createRequest);
        message.setCreatedAt(LocalDateTime.now());
        message.setTicketId(ticketId);
        message.setAuthorId(author.getId());

        if (ticket.getStatus() == TicketStatus.WAITING_USER
                && ticket.getCreatorId().equals(author.getId())) {
            ticket.setStatus(TicketStatus.ANSWERED);
            ticketRepository.save(ticket);
        }

        Message savedMessage = repository.save(message);

        return toResponse(savedMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessagesByTicket(Long ticketId) {
        ticketsMust(ticketId);

        return repository.findAllByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void ticketsMust(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new NotFoundException("Не найдена заявка с ID " + ticketId);
        }
    }

    private MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getText(),
                message.getCreatedAt(),
                message.getTicketId(),
                toSummary(message.getAuthorId())
        );
    }

    private UserSummary toSummary(Long userId) {
        return userRepository.findById(userId)
                .map(user -> new UserSummary(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        departmentName(user)
                ))
                .orElseThrow(() -> new NotFoundException("Не найден пользователь с ID " + userId));
    }

    private String departmentName(User user) {
        return departmentRepository.findById(user.getDepartmentId())
                .map(Department::getName)
                .orElse(null);
    }
}
