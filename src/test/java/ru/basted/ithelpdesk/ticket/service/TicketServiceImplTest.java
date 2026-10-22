package ru.basted.ithelpdesk.ticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.basted.ithelpdesk.category.entity.Category;
import ru.basted.ithelpdesk.category.repository.CategoryRepository;
import ru.basted.ithelpdesk.core.exception.ConflictException;
import ru.basted.ithelpdesk.core.exception.NotFoundException;
import ru.basted.ithelpdesk.core.model.BaseEntity;
import ru.basted.ithelpdesk.core.security.CurrentUser;
import ru.basted.ithelpdesk.department.entity.Department;
import ru.basted.ithelpdesk.message.service.MessageService;
import ru.basted.ithelpdesk.ticket.dto.TicketStatusChangeRequest;
import ru.basted.ithelpdesk.ticket.entity.Ticket;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;
import ru.basted.ithelpdesk.ticket.mapper.TicketMapper;
import ru.basted.ithelpdesk.ticket.repository.TicketRepository;
import ru.basted.ithelpdesk.ticket.service.AgentCapacityPolicy;
import ru.basted.ithelpdesk.user.entity.User;
import ru.basted.ithelpdesk.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {
    @Mock
    private TicketRepository repository;
    @Mock
    private TicketMapper mapper;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private MessageService messageService;
    @Mock
    private CurrentUser currentUser;

    private TicketServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TicketServiceImpl(
                repository, mapper, categoryRepository, messageService, currentUser,
                new AgentCapacityPolicy(repository), 24
        );
    }

    @Test
    void authorCanReopenResolvedTicketWithReason() {
        User author = user(1L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(TicketStatus.RESOLVED, author, user(2L, UserRole.AGENT));
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repository.save(ticket)).thenReturn(ticket);
        when(currentUser.get()).thenReturn(author);

        var response = service.changeStatus(
                1L, new TicketStatusChangeRequest(TicketStatus.IN_PROGRESS, "Проблема повторяется")
        );

        assertEquals(TicketStatus.IN_PROGRESS, response.status());
        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
    }

    @Test
    void reopenWithoutReasonThrowsConflict() {
        User author = user(1L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(TicketStatus.RESOLVED, author, user(2L, UserRole.AGENT));
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(currentUser.get()).thenReturn(author);

        assertThrows(
                ConflictException.class,
                () -> service.changeStatus(
                        1L, new TicketStatusChangeRequest(TicketStatus.IN_PROGRESS, " ")
                )
        );
        verify(repository, never()).save(any(Ticket.class));
    }

    @Test
    void agentCannotResolveForeignTicket() {
        User assignee = user(2L, UserRole.AGENT);
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS, user(1L, UserRole.EMPLOYEE), assignee);
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(currentUser.get()).thenReturn(user(3L, UserRole.AGENT));

        assertThrows(
                ConflictException.class,
                () -> service.changeStatus(
                        1L, new TicketStatusChangeRequest(TicketStatus.RESOLVED, null)
                )
        );
    }

    @Test
    void assignTicketSetsCurrentAgent() {
        User agent = user(2L, UserRole.AGENT);
        Ticket ticket = ticket(TicketStatus.NEW, user(1L, UserRole.EMPLOYEE), null);
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repository.save(ticket)).thenReturn(ticket);
        when(currentUser.get()).thenReturn(agent);

        var response = service.assignTicket(1L);

        assertEquals(TicketStatus.IN_PROGRESS, response.status());
        assertEquals(2L, response.assignee().id());
    }

    @Test
    void agentCannotAssignTicketBeyondLimit() {
        User agent = user(2L, UserRole.AGENT);
        agent.setTicketLimit(1);
        Ticket ticket = ticket(TicketStatus.NEW, user(1L, UserRole.EMPLOYEE), null);
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repository.countByAssigneeIdAndStatusIn(2L, AgentCapacityPolicy.ACTIVE_STATUSES))
                .thenReturn(1L);
        when(currentUser.get()).thenReturn(agent);

        assertThrows(ConflictException.class, () -> service.assignTicket(1L));
        verify(repository, never()).save(any(Ticket.class));
    }

    @Test
    void assignedOldTicketIsMarkedOverdue() {
        User agent = user(2L, UserRole.AGENT);
        Ticket ticket = ticket(TicketStatus.NEW, user(1L, UserRole.EMPLOYEE), null);
        ticket.setCreatedAt(LocalDateTime.now().minusHours(30));
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repository.save(ticket)).thenReturn(ticket);
        when(currentUser.get()).thenReturn(agent);

        var response = service.assignTicket(1L);

        assertEquals(true, response.overdue());
    }

    @Test
    void employeeSeesOnlyOwnTickets() {
        User employee = user(7L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(TicketStatus.NEW, employee, null);
        when(currentUser.get()).thenReturn(employee);
        when(repository.findAllByCreatorIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(ticket));

        var tickets = service.getAllTickets(null);

        assertEquals(1, tickets.size());
        verify(repository).findAllByCreatorIdOrderByCreatedAtDesc(7L);
        verify(repository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void employeeCannotReadForeignTicketDetails() {
        Ticket ticket = ticket(TicketStatus.NEW, user(1L, UserRole.EMPLOYEE), null);
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(currentUser.get()).thenReturn(user(3L, UserRole.EMPLOYEE));

        assertThrows(NotFoundException.class, () -> service.getTicketDetails(1L));
    }

    @Test
    void employeeCanReadOwnTicketDetails() {
        User author = user(1L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(TicketStatus.NEW, author, null);
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(currentUser.get()).thenReturn(author);
        when(messageService.getMessagesByTicket(1L)).thenReturn(List.of());

        var response = service.getTicketDetails(1L);

        assertEquals(1L, response.id());
    }

    @Test
    void agentSeesOwnAndNewTickets() {
        User agent = user(2L, UserRole.AGENT);
        Ticket own = ticket(TicketStatus.IN_PROGRESS, user(1L, UserRole.EMPLOYEE), agent);
        Ticket unassigned = ticket(TicketStatus.NEW, user(1L, UserRole.EMPLOYEE), null);
        when(currentUser.get()).thenReturn(agent);
        when(repository.findAllByAssigneeIdOrStatusOrderByCreatedAtDesc(2L, TicketStatus.NEW))
                .thenReturn(List.of(own, unassigned));

        var tickets = service.getAllTickets(null);

        assertEquals(2, tickets.size());
        verify(repository).findAllByAssigneeIdOrStatusOrderByCreatedAtDesc(2L, TicketStatus.NEW);
        verify(repository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void agentSeesOnlyNewTicketsWhenFilteringByNew() {
        User agent = user(2L, UserRole.AGENT);
        Ticket unassigned = ticket(TicketStatus.NEW, user(1L, UserRole.EMPLOYEE), null);
        when(currentUser.get()).thenReturn(agent);
        when(repository.findAllByStatusOrderByCreatedAtDesc(TicketStatus.NEW))
                .thenReturn(List.of(unassigned));

        var tickets = service.getAllTickets(TicketStatus.NEW);

        assertEquals(1, tickets.size());
        verify(repository).findAllByStatusOrderByCreatedAtDesc(TicketStatus.NEW);
        verify(repository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void managerSeesAllTickets() {
        User manager = user(3L, UserRole.MANAGER);
        Ticket ticket = ticket(TicketStatus.NEW, user(1L, UserRole.EMPLOYEE), null);
        when(currentUser.get()).thenReturn(manager);
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(ticket));

        var tickets = service.getAllTickets(null);

        assertEquals(1, tickets.size());
        verify(repository).findAllByOrderByCreatedAtDesc();
    }

    private Ticket ticket(TicketStatus status, User creator, User assignee) {
        Ticket ticket = new Ticket();
        setId(ticket, 1L);
        ticket.setTitle("Не работает почта");
        ticket.setDescription("Описание проблемы");
        ticket.setStatus(status);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setCreator(creator);
        ticket.setAssignee(assignee);
        ticket.setCategory(category());
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

    private Category category() {
        Category category = new Category();
        setId(category, 1L);
        category.setName("Оборудование");
        return category;
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
