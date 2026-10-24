package ru.basted.ithelpdesk.ticket.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ru.basted.ithelpdesk.category.entity.Category;
import ru.basted.ithelpdesk.category.repository.CategoryRepository;
import ru.basted.ithelpdesk.core.exception.ConflictException;
import ru.basted.ithelpdesk.core.exception.NotFoundException;
import ru.basted.ithelpdesk.core.security.CurrentUser;
import ru.basted.ithelpdesk.message.dto.MessageResponse;
import ru.basted.ithelpdesk.message.service.MessageService;
import ru.basted.ithelpdesk.ticket.dto.TicketCreateRequest;
import ru.basted.ithelpdesk.ticket.dto.TicketDeadlineExtensionRequest;
import ru.basted.ithelpdesk.ticket.dto.TicketDeadlineExtensionResponse;
import ru.basted.ithelpdesk.ticket.dto.TicketResponse;
import ru.basted.ithelpdesk.ticket.dto.TicketStatusChangeRequest;
import ru.basted.ithelpdesk.ticket.entity.Ticket;
import ru.basted.ithelpdesk.ticket.entity.TicketDeadlineExtension;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;
import ru.basted.ithelpdesk.ticket.mapper.TicketMapper;
import ru.basted.ithelpdesk.ticket.repository.TicketDeadlineExtensionRepository;
import ru.basted.ithelpdesk.ticket.repository.TicketRepository;
import ru.basted.ithelpdesk.user.dto.UserSummary;
import ru.basted.ithelpdesk.user.entity.User;
import ru.basted.ithelpdesk.user.entity.UserRole;

@Service
public class TicketServiceImpl implements TicketService {
    private static final Set<TicketStatus> RESOLVABLE_STATUSES =
            Set.of(TicketStatus.IN_PROGRESS, TicketStatus.WAITING_USER, TicketStatus.ANSWERED);

    private final TicketRepository repository;
    private final TicketDeadlineExtensionRepository extensionRepository;
    private final TicketMapper mapper;
    private final CategoryRepository categoryRepository;
    private final MessageService messageService;
    private final CurrentUser currentUser;
    private final AgentCapacityPolicy capacityPolicy;
    private final long overdueHours;

    public TicketServiceImpl(
            TicketRepository repository,
            TicketDeadlineExtensionRepository extensionRepository,
            TicketMapper mapper,
            CategoryRepository categoryRepository,
            MessageService messageService,
            CurrentUser currentUser,
            AgentCapacityPolicy capacityPolicy,
            @Value("${app.ticket.overdue-hours:24}") long overdueHours
    ) {
        this.repository = repository;
        this.extensionRepository = extensionRepository;
        this.mapper = mapper;
        this.categoryRepository = categoryRepository;
        this.messageService = messageService;
        this.currentUser = currentUser;
        this.capacityPolicy = capacityPolicy;
        this.overdueHours = overdueHours;
    }

    @Override
    @Transactional
    public TicketResponse createTicket(TicketCreateRequest createRequest) {
        User creator = currentUser.get();
        Category category = findCategory(createRequest.categoryId());

        Ticket ticket = mapper.toEntity(createRequest);
        ticket.setStatus(TicketStatus.NEW);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setCreator(creator);
        ticket.setCategory(category);

        return toResponse(repository.save(ticket), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets(TicketStatus status) {
        User viewer = currentUser.get();
        List<Ticket> tickets = switch (viewer.getRole()) {
            case EMPLOYEE -> findOwnTickets(viewer, status);
            case AGENT -> findAgentTickets(viewer, status);
            default -> findAllTickets(status);
        };

        return tickets.stream()
                .map(ticket -> toResponse(ticket, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketDetails(Long id) {
        Ticket ticket = findTicket(id);
        checkViewerCanRead(ticket);

        return toResponse(ticket, true);
    }

    @Override
    @Transactional
    public TicketResponse assignTicket(Long id) {
        User agent = currentUser.get();
        int limit = capacityPolicy.resolveLimit(agent);

        int claimed = repository.claimIfCapacityAvailable(id, agent.getId(), limit);
        if (claimed == 0) {
            Ticket ticket = findTicket(id);
            if (ticket.getStatus() != TicketStatus.NEW) {
                throw new ConflictException(
                        "Нельзя взять в работу заявку в статусе %s".formatted(ticket.getStatus())
                );
            }
            capacityPolicy.checkCanAssign(agent);
            throw new ConflictException(
                    "Не удалось взять заявку в работу. Обновите данные и повторите действие"
            );
        }

        return toResponse(findTicket(id), false);
    }

    @Override
    @Transactional
    public TicketResponse changeStatus(Long id, TicketStatusChangeRequest changeRequest) {
        Ticket ticket = findTicket(id);
        User user = currentUser.get();

        Ticket updated = switch (changeRequest.status()) {
            case WAITING_USER -> pauseTicket(ticket, user);
            case RESOLVED -> resolveTicket(ticket, user);
            case CLOSED -> closeTicket(ticket, user);
            case IN_PROGRESS -> resumeOrReopenTicket(ticket, user, changeRequest.reason());
            case NEW -> throw new IllegalArgumentException(
                    "Недопустимый целевой статус NEW для изменения заявки"
            );
            default -> throw new IllegalArgumentException("Новый статус заявки обязателен");
        };

        return toResponse(updated, false);
    }

    @Override
    @Transactional
    public TicketResponse extendDeadline(Long id, TicketDeadlineExtensionRequest extensionRequest) {
        Ticket ticket = findTicket(id);
        User user = currentUser.get();

        checkRule(AgentCapacityPolicy.ACTIVE_STATUSES.contains(ticket.getStatus()),
                "Продлить крайний срок можно только для активной заявки");
        if (user.getRole() == UserRole.AGENT) {
            checkOwnership(user, ticket.getAssignee(),
                    "Продлить крайний срок может только назначенный исполнитель");
        } else {
            checkRule(user.getRole() == UserRole.MANAGER || user.getRole() == UserRole.ADMIN,
                    "Продлить крайний срок может исполнитель, руководитель или администратор");
        }

        int currentExtension = ticket.getExtensionHours() == null ? 0 : ticket.getExtensionHours();
        int totalExtension = currentExtension + extensionRequest.hours();
        checkRule(totalExtension <= 720,
                "Суммарное продление крайнего срока не может превышать 720 часов");

        TicketDeadlineExtension extension = new TicketDeadlineExtension();
        extension.setHours(extensionRequest.hours());
        extension.setReason(extensionRequest.reason());
        extension.setCreatedAt(LocalDateTime.now());
        extension.setTicket(ticket);
        extension.setCreatedBy(user);
        extensionRepository.save(extension);

        ticket.setExtensionHours(totalExtension);
        return toResponse(ticket, true);
    }

    private Ticket pauseTicket(Ticket ticket, User user) {
        checkRule(ticket.getStatus() == TicketStatus.IN_PROGRESS,
                "Приостановить заявку может только исполнитель в статусе IN_PROGRESS");
        checkOwnership(user, ticket.getAssignee(),
                "Приостановить заявку может только назначенный исполнитель");

        ticket.setStatus(TicketStatus.WAITING_USER);
        return ticket;
    }

    private Ticket resolveTicket(Ticket ticket, User user) {
        checkRule(RESOLVABLE_STATUSES.contains(ticket.getStatus()),
                "Отметить заявку решённой можно только из статусов IN_PROGRESS, WAITING_USER или ANSWERED");
        checkOwnership(user, ticket.getAssignee(),
                "Отметить заявку решённой может только назначенный исполнитель");

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());
        ticket.setClosedAt(null);
        return ticket;
    }

    private Ticket closeTicket(Ticket ticket, User user) {
        checkRule(ticket.getStatus() == TicketStatus.RESOLVED,
                "Закрыть заявку можно только из статуса RESOLVED");
        checkOwnership(user, ticket.getCreator(),
                "Закрыть заявку может только её автор");

        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setClosedAt(LocalDateTime.now());
        return ticket;
    }

    private Ticket resumeOrReopenTicket(Ticket ticket, User user, String reason) {
        return switch (ticket.getStatus()) {
            case ANSWERED -> resumeWork(ticket, user);
            case RESOLVED -> reopenTicket(ticket, user, reason);
            default -> throw new ConflictException(
                    "Вернуть заявку на доработку можно только из статуса RESOLVED"
            );
        };
    }

    private Ticket resumeWork(Ticket ticket, User user) {
        checkOwnership(user, ticket.getAssignee(),
                "Возобновить заявку может только назначенный исполнитель");

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        return ticket;
    }

    private Ticket reopenTicket(Ticket ticket, User user, String reason) {
        checkOwnership(user, ticket.getCreator(),
                "Вернуть заявку на доработку может только её автор");
        checkRule(StringUtils.hasText(reason),
                "Для возврата заявки на доработку требуется причина");

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setResolvedAt(null);
        ticket.setClosedAt(null);
        return ticket;
    }

    private void checkRule(boolean condition, String message) {
        if (!condition) {
            throw new ConflictException(message);
        }
    }

    private void checkOwnership(User user, User expected, String message) {
        checkRule(expected != null && user.getId().equals(expected.getId()), message);
    }

    private void checkViewerCanRead(Ticket ticket) {
        User viewer = currentUser.get();

        if (viewer.getRole() == UserRole.EMPLOYEE
                && !viewer.getId().equals(ticket.getCreator().getId())) {
            throw new NotFoundException("Не найдена заявка с ID %d".formatted(ticket.getId()));
        }
    }

    private List<Ticket> findOwnTickets(User viewer, TicketStatus status) {
        return status == null
                ? repository.findAllByCreatorIdOrderByCreatedAtDesc(viewer.getId())
                : repository.findAllByCreatorIdAndStatusOrderByCreatedAtDesc(viewer.getId(), status);
    }

    private List<Ticket> findAgentTickets(User viewer, TicketStatus status) {
        if (status == null) {
            return repository.findAllByAssigneeIdOrStatusOrderByCreatedAtDesc(
                    viewer.getId(), TicketStatus.NEW
            );
        }
        if (status == TicketStatus.NEW) {
            return repository.findAllByStatusOrderByCreatedAtDesc(TicketStatus.NEW);
        }
        return repository.findAllByAssigneeIdAndStatusOrderByCreatedAtDesc(viewer.getId(), status);
    }

    private List<Ticket> findAllTickets(TicketStatus status) {
        return status == null
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findAllByStatusOrderByCreatedAtDesc(status);
    }

    private Ticket findTicket(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Не найдена заявка с ID %d".formatted(id)));
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Не найдена категория с ID %d".formatted(categoryId)
                ));
    }

    private TicketResponse toResponse(Ticket ticket, boolean withMessages) {
        UserSummary creator = toSummary(ticket.getCreator());
        UserSummary assignee = ticket.getAssignee() == null
                ? null
                : toSummary(ticket.getAssignee());

        List<MessageResponse> messages = withMessages
                ? messageService.getMessagesForTicket(ticket.getId())
                : List.of();
        List<TicketDeadlineExtensionResponse> deadlineExtensions = withMessages
                ? extensionRepository.findAllByTicketIdOrderByCreatedAtAsc(ticket.getId())
                        .stream()
                        .map(this::toExtensionResponse)
                        .toList()
                : List.of();

        LocalDateTime dueAt = TicketSlaPolicy.dueAt(ticket, overdueHours);
        boolean overdue = TicketSlaPolicy.isOverdue(ticket, overdueHours, LocalDateTime.now());

        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                creator,
                assignee,
                ticket.getCategory().getName(),
                overdue,
                dueAt,
                ticket.getResolvedAt(),
                ticket.getClosedAt(),
                ticket.getExtensionHours(),
                deadlineExtensions,
                messages
        );
    }

    private TicketDeadlineExtensionResponse toExtensionResponse(TicketDeadlineExtension extension) {
        return new TicketDeadlineExtensionResponse(
                extension.getId(),
                extension.getHours(),
                extension.getReason(),
                extension.getCreatedAt(),
                toSummary(extension.getCreatedBy())
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
