package ru.basted.ithelpdesk.comment.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.basted.ithelpdesk.comment.dto.CommentCreateRequest;
import ru.basted.ithelpdesk.comment.dto.CommentResponse;
import ru.basted.ithelpdesk.comment.entity.Comment;
import ru.basted.ithelpdesk.comment.mapper.CommentMapper;
import ru.basted.ithelpdesk.comment.repository.CommentRepository;
import ru.basted.ithelpdesk.core.exception.ConflictException;
import ru.basted.ithelpdesk.core.exception.NotFoundException;
import ru.basted.ithelpdesk.core.security.CurrentUser;
import ru.basted.ithelpdesk.department.entity.Department;
import ru.basted.ithelpdesk.department.repository.DepartmentRepository;
import ru.basted.ithelpdesk.ticket.entity.Ticket;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;
import ru.basted.ithelpdesk.ticket.repository.TicketRepository;
import ru.basted.ithelpdesk.user.dto.UserSummary;
import ru.basted.ithelpdesk.user.entity.User;
import ru.basted.ithelpdesk.user.entity.UserRole;
import ru.basted.ithelpdesk.user.repository.UserRepository;

@Service
public class CommentServiceImpl implements CommentService {
    private final CommentRepository repository;
    private final CommentMapper mapper;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CurrentUser currentUser;

    public CommentServiceImpl(
            CommentRepository repository,
            CommentMapper mapper,
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
    public CommentResponse addComment(Long ticketId, CommentCreateRequest createRequest) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Не найдена заявка с ID " + ticketId));

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ConflictException("Нельзя добавлять комментарии к закрытой заявке");
        }

        User author = currentUser.get();

        if (author.getRole() == UserRole.AGENT
                && !author.getId().equals(ticket.getAssigneeId())) {
            throw new ConflictException(
                    "Специалист может писать сообщения только в заявке, взятой им в работу"
            );
        }

        Comment comment = mapper.toEntity(createRequest);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setTicketId(ticketId);
        comment.setAuthorId(author.getId());

        if (ticket.getStatus() == TicketStatus.WAITING_USER
                && ticket.getCreatorId().equals(author.getId())) {
            ticket.setStatus(TicketStatus.ANSWERED);
            ticketRepository.save(ticket);
        }

        Comment savedComment = repository.save(comment);

        return toResponse(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByTicket(Long ticketId) {
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

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getText(),
                comment.getCreatedAt(),
                comment.getTicketId(),
                toSummary(comment.getAuthorId())
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