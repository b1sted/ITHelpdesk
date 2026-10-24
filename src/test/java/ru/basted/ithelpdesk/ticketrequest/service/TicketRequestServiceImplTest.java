package ru.basted.ithelpdesk.ticketrequest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.basted.ithelpdesk.category.entity.Category;
import ru.basted.ithelpdesk.core.exception.ConflictException;
import ru.basted.ithelpdesk.core.model.BaseEntity;
import ru.basted.ithelpdesk.core.security.CurrentUser;
import ru.basted.ithelpdesk.department.entity.Department;
import ru.basted.ithelpdesk.ticket.entity.Ticket;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;
import ru.basted.ithelpdesk.ticket.repository.TicketRepository;
import ru.basted.ithelpdesk.ticket.service.AgentCapacityPolicy;
import ru.basted.ithelpdesk.ticketrequest.dto.TicketRequestCreateRequest;
import ru.basted.ithelpdesk.ticketrequest.dto.TicketRequestDecisionRequest;
import ru.basted.ithelpdesk.ticketrequest.entity.TicketRequest;
import ru.basted.ithelpdesk.ticketrequest.entity.TicketRequestStatus;
import ru.basted.ithelpdesk.ticketrequest.entity.TicketRequestType;
import ru.basted.ithelpdesk.ticketrequest.mapper.TicketRequestMapper;
import ru.basted.ithelpdesk.ticketrequest.repository.TicketRequestRepository;
import ru.basted.ithelpdesk.user.entity.User;
import ru.basted.ithelpdesk.user.entity.UserRole;
import ru.basted.ithelpdesk.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TicketRequestServiceImplTest {
    @Mock
    private TicketRequestRepository repository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TicketRequestMapper mapper;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private AgentCapacityPolicy capacityPolicy;

    private TicketRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TicketRequestServiceImpl(
                repository, ticketRepository, userRepository, mapper, currentUser, capacityPolicy
        );
    }

    @Test
    void assignedAgentCreatesReleaseRequest() {
        User agent = user(2L, UserRole.AGENT);
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS, user(1L, UserRole.EMPLOYEE), agent);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(currentUser.get()).thenReturn(agent);
        when(repository.existsByTicketIdAndStatus(1L, TicketRequestStatus.PENDING)).thenReturn(false);
        when(mapper.toEntity(any(TicketRequestCreateRequest.class))).thenAnswer(invocation -> {
            TicketRequestCreateRequest dto = invocation.getArgument(0);
            TicketRequest request = new TicketRequest();
            request.setType(dto.type());
            request.setReason(dto.reason());
            return request;
        });
        when(repository.save(any(TicketRequest.class))).thenAnswer(invocation -> {
            TicketRequest request = invocation.getArgument(0);
            setId(request, 10L);
            return request;
        });

        var response = service.createRequest(
                1L, new TicketRequestCreateRequest(TicketRequestType.RELEASE, "Нужна помощь", null)
        );

        assertEquals(TicketRequestStatus.PENDING, response.status());
        assertEquals(TicketRequestType.RELEASE, response.type());
    }

    @Test
    void approvedReleaseReturnsTicketToQueue() {
        User agent = user(2L, UserRole.AGENT);
        User manager = user(5L, UserRole.MANAGER);
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS, user(1L, UserRole.EMPLOYEE), agent);
        TicketRequest request = request(TicketRequestType.RELEASE, agent, null, ticket);

        when(repository.findById(10L)).thenReturn(Optional.of(request));
        when(currentUser.get()).thenReturn(manager);
        when(repository.save(request)).thenReturn(request);

        service.decide(10L, new TicketRequestDecisionRequest(TicketRequestStatus.APPROVED, "Согласовано", null));

        assertEquals(TicketStatus.NEW, ticket.getStatus());
        assertNull(ticket.getAssignee());
        assertEquals(TicketRequestStatus.APPROVED, request.getStatus());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void approvedReassignAssignsTargetAgent() {
        User requester = user(2L, UserRole.AGENT);
        User target = user(3L, UserRole.AGENT);
        User manager = user(5L, UserRole.MANAGER);
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS, user(1L, UserRole.EMPLOYEE), requester);
        TicketRequest request = request(TicketRequestType.REASSIGN, requester, target, ticket);

        when(repository.findById(10L)).thenReturn(Optional.of(request));
        when(currentUser.get()).thenReturn(manager);
        when(repository.save(request)).thenReturn(request);

        service.decide(10L, new TicketRequestDecisionRequest(TicketRequestStatus.APPROVED, null, null));

        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        assertEquals(3L, ticket.getAssignee().getId());
        verify(capacityPolicy).checkCanAssign(target);
    }

    @Test
    void approvedReassignWithoutTargetUsesManagerChoice() {
        User requester = user(2L, UserRole.AGENT);
        User target = user(3L, UserRole.AGENT);
        User manager = user(5L, UserRole.MANAGER);
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS, user(1L, UserRole.EMPLOYEE), requester);
        TicketRequest request = request(TicketRequestType.REASSIGN, requester, null, ticket);

        when(repository.findById(10L)).thenReturn(Optional.of(request));
        when(currentUser.get()).thenReturn(manager);
        when(userRepository.findById(3L)).thenReturn(Optional.of(target));
        when(repository.save(request)).thenReturn(request);

        service.decide(10L, new TicketRequestDecisionRequest(TicketRequestStatus.APPROVED, null, 3L));

        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        assertEquals(3L, ticket.getAssignee().getId());
        assertEquals(3L, request.getTargetAssignee().getId());
        verify(capacityPolicy).checkCanAssign(target);
    }

    @Test
    void approvedReassignWithoutAnyTargetThrowsConflict() {
        User requester = user(2L, UserRole.AGENT);
        User manager = user(5L, UserRole.MANAGER);
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS, user(1L, UserRole.EMPLOYEE), requester);
        TicketRequest request = request(TicketRequestType.REASSIGN, requester, null, ticket);

        when(repository.findById(10L)).thenReturn(Optional.of(request));
        when(currentUser.get()).thenReturn(manager);

        assertThrows(
                ConflictException.class,
                () -> service.decide(
                        10L, new TicketRequestDecisionRequest(TicketRequestStatus.APPROVED, null, null)
                )
        );
    }

    @Test
    void rejectedRequestDoesNotChangeTicket() {
        User requester = user(2L, UserRole.AGENT);
        User manager = user(5L, UserRole.MANAGER);
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS, user(1L, UserRole.EMPLOYEE), requester);
        TicketRequest request = request(TicketRequestType.RELEASE, requester, null, ticket);

        when(repository.findById(10L)).thenReturn(Optional.of(request));
        when(currentUser.get()).thenReturn(manager);
        when(repository.save(request)).thenReturn(request);

        service.decide(10L, new TicketRequestDecisionRequest(TicketRequestStatus.REJECTED, "Нет оснований", null));

        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        assertEquals(2L, ticket.getAssignee().getId());
        assertEquals(TicketRequestStatus.REJECTED, request.getStatus());
    }

    private TicketRequest request(
            TicketRequestType type,
            User requester,
            User target,
            Ticket ticket
    ) {
        TicketRequest request = new TicketRequest();
        setId(request, 10L);
        request.setType(type);
        request.setStatus(TicketRequestStatus.PENDING);
        request.setReason("Обоснование");
        request.setCreatedAt(LocalDateTime.now());
        request.setRequester(requester);
        request.setTargetAssignee(target);
        request.setTicket(ticket);
        return request;
    }

    private Ticket ticket(TicketStatus status, User creator, User assignee) {
        Ticket ticket = new Ticket();
        setId(ticket, 1L);
        ticket.setTitle("Заявка");
        ticket.setDescription("Описание");
        ticket.setStatus(status);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setCreator(creator);
        ticket.setAssignee(assignee);
        ticket.setCategory(new Category());
        return ticket;
    }

    private User user(Long id, UserRole role) {
        User user = new User();
        setId(user, id);
        user.setFullName("Пользователь " + id);
        user.setEmail("user" + id + "@company.ru");
        user.setRole(role);
        user.setDepartment(department());
        return user;
    }

    private Department department() {
        Department department = new Department();
        setId(department, 1L);
        department.setName("IT Service Desk");
        return department;
    }

    private void setId(BaseEntity entity, Long id) {
        try {
            Field field = BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
