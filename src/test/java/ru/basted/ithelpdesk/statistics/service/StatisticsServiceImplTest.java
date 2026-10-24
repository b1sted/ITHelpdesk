package ru.basted.ithelpdesk.statistics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.basted.ithelpdesk.category.entity.Category;
import ru.basted.ithelpdesk.core.model.BaseEntity;
import ru.basted.ithelpdesk.department.entity.Department;
import ru.basted.ithelpdesk.statistics.dto.StatisticsGranularity;
import ru.basted.ithelpdesk.ticket.entity.Ticket;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;
import ru.basted.ithelpdesk.ticket.repository.TicketRepository;
import ru.basted.ithelpdesk.user.entity.User;
import ru.basted.ithelpdesk.user.entity.UserRole;
import ru.basted.ithelpdesk.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private TicketRepository ticketRepository;

    private StatisticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StatisticsServiceImpl(userRepository, ticketRepository, 24);
    }

    @Test
    void aggregatesAgentTicketsAndOverdueCount() {
        User agent = user(2L, UserRole.AGENT);
        agent.setTicketLimit(5);
        agent.setTimeLimitHours(12);

        when(userRepository.findAllByRoleOrderByFullNameAsc(UserRole.AGENT))
                .thenReturn(List.of(agent));
        when(ticketRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
                ticket(TicketStatus.IN_PROGRESS, agent, LocalDateTime.now().minusHours(30)),
                ticket(TicketStatus.WAITING_USER, agent, LocalDateTime.now().minusHours(30)),
                ticket(TicketStatus.RESOLVED, agent, LocalDateTime.now().minusHours(30)),
                ticket(TicketStatus.CLOSED, agent, LocalDateTime.now().minusHours(1))
        ));

        var statistics = service.getAgentStatistics();

        assertEquals(1, statistics.size());
        assertEquals(5, statistics.getFirst().ticketLimit());
        assertEquals(12, statistics.getFirst().timeLimitHours());
        assertEquals(4, statistics.getFirst().totalAssigned());
        assertEquals(2, statistics.getFirst().activeTickets());
        assertEquals(1, statistics.getFirst().resolvedTickets());
        assertEquals(1, statistics.getFirst().closedTickets());
        assertEquals(1, statistics.getFirst().overdueTickets());
    }

    @Test
    void aggregatesMonthlyPeriodStatistics() {
        User agent = user(2L, UserRole.AGENT);
        LocalDateTime created = LocalDateTime.of(LocalDateTime.now().getYear(), 1, 10, 10, 0);
        Ticket ticket = ticket(TicketStatus.CLOSED, agent, created);
        ticket.setResolvedAt(created.plusHours(20));
        ticket.setClosedAt(created.plusHours(24));

        when(ticketRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(ticket));

        var statistics = service.getPeriodStatistics(StatisticsGranularity.MONTH);

        assertEquals(12, statistics.buckets().size());
        assertEquals(1, statistics.totalCreated());
        assertEquals(1, statistics.totalResolved());
        assertEquals(1, statistics.totalClosed());
        assertEquals(20.0, statistics.averageResolutionHours());
        assertEquals(1, statistics.buckets().getFirst().created());
    }

    @Test
    void distributesMonthlyStatisticsAcrossCorrectMonths() {
        User agent = user(2L, UserRole.AGENT);
        int year = LocalDateTime.now().getYear();
        LocalDateTime created = LocalDateTime.of(year, 10, 10, 10, 0);
        Ticket ticket = ticket(TicketStatus.CLOSED, agent, created);
        ticket.setResolvedAt(created.plusHours(20));
        ticket.setClosedAt(created.plusHours(24));

        when(ticketRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(ticket));

        var statistics = service.getPeriodStatistics(StatisticsGranularity.MONTH);

        assertEquals(0, statistics.buckets().getFirst().created());
        assertEquals(1, statistics.buckets().get(9).created());
        assertEquals(1, statistics.buckets().get(9).resolved());
        assertEquals(1, statistics.buckets().get(9).closed());
    }

    private Ticket ticket(TicketStatus status, User assignee, LocalDateTime createdAt) {
        Ticket ticket = new Ticket();
        ticket.setTitle("Заявка");
        ticket.setDescription("Описание");
        ticket.setStatus(status);
        ticket.setCreatedAt(createdAt);
        ticket.setAssignee(assignee);
        ticket.setCreator(assignee);
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
