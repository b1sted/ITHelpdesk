package ru.basted.ithelpdesk.ticket.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ru.basted.ithelpdesk.category.entity.Category;
import ru.basted.ithelpdesk.category.repository.CategoryRepository;
import ru.basted.ithelpdesk.core.exception.ConflictException;
import ru.basted.ithelpdesk.core.exception.NotFoundException;
import ru.basted.ithelpdesk.core.security.CurrentUser;
import ru.basted.ithelpdesk.department.entity.Department;
import ru.basted.ithelpdesk.department.repository.DepartmentRepository;
import ru.basted.ithelpdesk.message.dto.MessageResponse;
import ru.basted.ithelpdesk.message.service.MessageService;
import ru.basted.ithelpdesk.ticket.dto.TicketCreateRequest;
import ru.basted.ithelpdesk.ticket.dto.TicketResponse;
import ru.basted.ithelpdesk.ticket.dto.TicketStatusChangeRequest;
import ru.basted.ithelpdesk.ticket.entity.Ticket;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;
import ru.basted.ithelpdesk.ticket.mapper.TicketMapper;
import ru.basted.ithelpdesk.ticket.repository.TicketRepository;
import ru.basted.ithelpdesk.user.dto.UserSummary;
import ru.basted.ithelpdesk.user.entity.User;
import ru.basted.ithelpdesk.user.repository.UserRepository;

@Service
public class TicketServiceImpl implements TicketService {
    private final TicketRepository repository;
    private final TicketMapper mapper;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final MessageService messageService;
    private final CurrentUser currentUser;

    public TicketServiceImpl(
            TicketRepository repository,
            TicketMapper mapper,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            MessageService messageService,
            CurrentUser currentUser
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.messageService = messageService;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional
    public TicketResponse createTicket(TicketCreateRequest createRequest) {
        User creator = currentUser.get();
        validateCategory(createRequest.categoryId());

        Ticket ticket = mapper.toEntity(createRequest);
        ticket.setStatus(TicketStatus.NEW);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setCreatorId(creator.getId());

        Ticket savedTicket = repository.save(ticket);

        return toResponse(savedTicket, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets(TicketStatus status) {
        List<Ticket> tickets;
        if (status != null) {
            tickets = repository.findAllByStatusOrderByCreatedAtDesc(status);
        } else {
            tickets = repository.findAllByOrderByCreatedAtDesc();
        }

        return tickets.stream()
                .map(ticket -> toResponse(ticket, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketDetails(Long id) {
        Ticket ticket = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Не найдена заявка с ID " + id));

        return toResponse(ticket, true);
    }

    @Override
    @Transactional
    public TicketResponse assignTicket(Long id) {
        Ticket ticket = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Не найдена заявка с ID " + id));

        if (ticket.getStatus() != TicketStatus.NEW) {
            throw new ConflictException(
                    "Нельзя взять в работу заявку в статусе " + ticket.getStatus()
            );
        }

        User assignee = currentUser.get();
        ticket.setAssigneeId(assignee.getId());
        ticket.setStatus(TicketStatus.IN_PROGRESS);

        return toResponse(repository.save(ticket), false);
    }

    @Override
    @Transactional
    public TicketResponse changeStatus(Long id, TicketStatusChangeRequest changeRequest) {
        Ticket ticket = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Не найдена заявка с ID " + id));

        User user = currentUser.get();
        TicketStatus target = changeRequest.status();

        switch (target) {
            case WAITING_USER -> pauseTicket(ticket, user);
            case RESOLVED -> resolveTicket(ticket, user);
            case CLOSED -> closeTicket(ticket, user);
            case IN_PROGRESS -> resumeOrReopenTicket(ticket, user, changeRequest.reason());
            default -> throw new IllegalArgumentException(
                    "Недопустимый целевой статус " + target + " для изменения заявки"
            );
        }

        return toResponse(repository.save(ticket), false);
    }

    private void pauseTicket(Ticket ticket, User user) {
        checkRule(ticket.getStatus() == TicketStatus.IN_PROGRESS,
                "Приостановить заявку может только исполнитель в статусе IN_PROGRESS");
        checkRule(user.getId().equals(ticket.getAssigneeId()),
                "Приостановить заявку может только назначенный исполнитель");

        ticket.setStatus(TicketStatus.WAITING_USER);
    }

    private void resolveTicket(Ticket ticket, User user) {
        checkRule(ticket.getStatus() == TicketStatus.IN_PROGRESS
                        || ticket.getStatus() == TicketStatus.WAITING_USER
                        || ticket.getStatus() == TicketStatus.ANSWERED,
                "Отметить заявку решённой можно только из статусов IN_PROGRESS, WAITING_USER или ANSWERED");
        checkRule(user.getId().equals(ticket.getAssigneeId()),
                "Отметить заявку решённой может только назначенный исполнитель");

        ticket.setStatus(TicketStatus.RESOLVED);
    }

    private void closeTicket(Ticket ticket, User user) {
        checkRule(ticket.getStatus() == TicketStatus.RESOLVED,
                "Закрыть заявку можно только из статуса RESOLVED");
        checkRule(user.getId().equals(ticket.getCreatorId()),
                "Закрыть заявку может только её автор");

        ticket.setStatus(TicketStatus.CLOSED);
    }

    private void resumeOrReopenTicket(Ticket ticket, User user, String reason) {
        if (ticket.getStatus() == TicketStatus.ANSWERED) {
            checkRule(user.getId().equals(ticket.getAssigneeId()),
                    "Возобновить заявку может только назначенный исполнитель");
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            return;
        }

        checkRule(ticket.getStatus() == TicketStatus.RESOLVED,
                "Вернуть заявку на доработку можно только из статуса RESOLVED");
        checkRule(user.getId().equals(ticket.getCreatorId()),
                "Вернуть заявку на доработку может только её автор");
        checkRule(StringUtils.hasText(reason),
                "Для возврата заявки на доработку требуется причина");

        ticket.setStatus(TicketStatus.IN_PROGRESS);
    }

    private void checkRule(boolean condition, String message) {
        if (!condition) {
            throw new ConflictException(message);
        }
    }

    private void validateCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("Не найдена категория с ID " + categoryId);
        }
    }

    private TicketResponse toResponse(Ticket ticket, boolean withMessages) {
        UserSummary creator = toSummary(ticket.getCreatorId());
        UserSummary assignee = ticket.getAssigneeId() == null
                ? null
                : toSummary(ticket.getAssigneeId());

        String categoryName = categoryRepository.findById(ticket.getCategoryId())
                .map(Category::getName)
                .orElse(null);

        List<MessageResponse> messages = withMessages
                ? messageService.getMessagesByTicket(ticket.getId())
                : List.of();

        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                creator,
                assignee,
                categoryName,
                messages
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