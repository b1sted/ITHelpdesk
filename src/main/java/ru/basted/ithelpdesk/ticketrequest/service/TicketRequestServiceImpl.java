package ru.basted.ithelpdesk.ticketrequest.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.basted.ithelpdesk.core.exception.ConflictException;
import ru.basted.ithelpdesk.core.exception.NotFoundException;
import ru.basted.ithelpdesk.core.security.CurrentUser;
import ru.basted.ithelpdesk.ticket.entity.Ticket;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;
import ru.basted.ithelpdesk.ticket.repository.TicketRepository;
import ru.basted.ithelpdesk.ticket.service.AgentCapacityPolicy;
import ru.basted.ithelpdesk.ticketrequest.dto.TicketRequestCreateRequest;
import ru.basted.ithelpdesk.ticketrequest.dto.TicketRequestDecisionRequest;
import ru.basted.ithelpdesk.ticketrequest.dto.TicketRequestResponse;
import ru.basted.ithelpdesk.ticketrequest.entity.TicketRequest;
import ru.basted.ithelpdesk.ticketrequest.entity.TicketRequestStatus;
import ru.basted.ithelpdesk.ticketrequest.entity.TicketRequestType;
import ru.basted.ithelpdesk.ticketrequest.mapper.TicketRequestMapper;
import ru.basted.ithelpdesk.ticketrequest.repository.TicketRequestRepository;
import ru.basted.ithelpdesk.user.dto.UserSummary;
import ru.basted.ithelpdesk.user.entity.User;
import ru.basted.ithelpdesk.user.entity.UserRole;
import ru.basted.ithelpdesk.user.repository.UserRepository;

@Service
public class TicketRequestServiceImpl implements TicketRequestService {
    private final TicketRequestRepository repository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketRequestMapper mapper;
    private final CurrentUser currentUser;
    private final AgentCapacityPolicy capacityPolicy;

    public TicketRequestServiceImpl(
            TicketRequestRepository repository,
            TicketRepository ticketRepository,
            UserRepository userRepository,
            TicketRequestMapper mapper,
            CurrentUser currentUser,
            AgentCapacityPolicy capacityPolicy
    ) {
        this.repository = repository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.capacityPolicy = capacityPolicy;
    }

    @Override
    @Transactional
    public TicketRequestResponse createRequest(Long ticketId, TicketRequestCreateRequest request) {
        Ticket ticket = findTicket(ticketId);
        User requester = currentUser.get();

        if (requester.getRole() != UserRole.AGENT
                || ticket.getAssignee() == null
                || !requester.getId().equals(ticket.getAssignee().getId())) {
            throw new ConflictException(
                    "Запрос может создать только специалист, назначенный на заявку"
            );
        }

        if (!AgentCapacityPolicy.ACTIVE_STATUSES.contains(ticket.getStatus())) {
            throw new ConflictException(
                    "Запрос можно создать только по активной заявке"
            );
        }

        if (repository.existsByTicketIdAndStatus(ticketId, TicketRequestStatus.PENDING)) {
            throw new ConflictException("По заявке уже есть запрос, ожидающий решения руководителя");
        }

        User target = null;
        if (request.type() == TicketRequestType.REASSIGN) {
            if (request.targetAssigneeId() == null) {
                throw new ConflictException("Для переназначения требуется выбрать специалиста");
            }
            target = findAgent(request.targetAssigneeId());
            if (target.getId().equals(requester.getId())) {
                throw new ConflictException("Нельзя переназначить заявку на самого себя");
            }
        }

        TicketRequest ticketRequest = mapper.toEntity(request);
        ticketRequest.setStatus(TicketRequestStatus.PENDING);
        ticketRequest.setCreatedAt(LocalDateTime.now());
        ticketRequest.setTicket(ticket);
        ticketRequest.setRequester(requester);
        ticketRequest.setTargetAssignee(target);

        return toResponse(repository.save(ticketRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketRequestResponse> getTicketRequests(Long ticketId) {
        ensureTicketExists(ticketId);

        return repository.findAllByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketRequestResponse> getRequests(TicketRequestStatus status) {
        List<TicketRequest> requests = status == null
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findAllByStatusOrderByCreatedAtDesc(status);

        return requests.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketRequestResponse decide(Long requestId, TicketRequestDecisionRequest decision) {
        TicketRequest request = repository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(
                        "Не найден запрос с ID %d".formatted(requestId)
                ));

        if (request.getStatus() != TicketRequestStatus.PENDING) {
            throw new ConflictException("Запрос уже рассмотрен руководителем");
        }

        if (decision.status() != TicketRequestStatus.APPROVED
                && decision.status() != TicketRequestStatus.REJECTED) {
            throw new IllegalArgumentException("Решение по запросу должно быть APPROVED или REJECTED");
        }

        User manager = currentUser.get();
        if (manager.getRole() != UserRole.MANAGER && manager.getRole() != UserRole.ADMIN) {
            throw new ConflictException("Решение по запросу может принять только руководитель");
        }

        if (decision.status() == TicketRequestStatus.APPROVED) {
            applyApprovedRequest(request);
        }

        request.setStatus(decision.status());
        request.setDecisionComment(decision.comment());
        request.setDecidedBy(manager);
        request.setDecidedAt(LocalDateTime.now());

        return toResponse(repository.save(request));
    }

    private void applyApprovedRequest(TicketRequest request) {
        Ticket ticket = request.getTicket();

        if (!AgentCapacityPolicy.ACTIVE_STATUSES.contains(ticket.getStatus())
                || ticket.getAssignee() == null
                || !request.getRequester().getId().equals(ticket.getAssignee().getId())) {
            throw new ConflictException(
                    "Заявка уже изменилась и не может быть обработана по этому запросу"
            );
        }

        if (request.getType() == TicketRequestType.RELEASE) {
            ticket.setAssignee(null);
            ticket.setStatus(TicketStatus.NEW);
        } else {
            User target = request.getTargetAssignee();
            capacityPolicy.checkCanAssign(target);
            ticket.setAssignee(target);
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }

        ticketRepository.save(ticket);
    }

    private void ensureTicketExists(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new NotFoundException("Не найдена заявка с ID %d".formatted(ticketId));
        }
    }

    private Ticket findTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException(
                        "Не найдена заявка с ID %d".formatted(ticketId)
                ));
    }

    private User findAgent(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Не найден пользователь с ID %d".formatted(id)
                ));

        if (user.getRole() != UserRole.AGENT) {
            throw new ConflictException("Переназначить заявку можно только на специалиста поддержки");
        }

        return user;
    }

    private TicketRequestResponse toResponse(TicketRequest request) {
        return new TicketRequestResponse(
                request.getId(),
                request.getType(),
                request.getStatus(),
                request.getReason(),
                request.getDecisionComment(),
                request.getCreatedAt(),
                request.getDecidedAt(),
                request.getTicket().getId(),
                toSummary(request.getRequester()),
                request.getTargetAssignee() == null ? null : toSummary(request.getTargetAssignee()),
                request.getDecidedBy() == null ? null : toSummary(request.getDecidedBy())
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
