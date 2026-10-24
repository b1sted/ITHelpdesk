package ru.basted.ithelpdesk.statistics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
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
    void aggregatesMonthlyPeriodStatisticsFromFirstActivity() {
        User agent = user(2L, UserRole.AGENT);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = LocalDateTime.of(now.getYear(), 1, 10, 10, 0);
        Ticket ticket = ticket(TicketStatus.CLOSED, agent, created);
        ticket.setResolvedAt(created.plusHours(20));
        ticket.setClosedAt(created.plusHours(24));

        when(ticketRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(ticket));

        var statistics = service.getPeriodStatistics(StatisticsGranularity.MONTH);

        assertEquals(now.getMonthValue(), statistics.buckets().size());
        assertEquals(1, statistics.totalCreated());
        assertEquals(1, statistics.totalResolved());
        assertEquals(1, statistics.totalClosed());
        assertEquals(20.0, statistics.averageResolutionHours());
        assertEquals(1, statistics.buckets().getFirst().created());
    }

    @Test
    void distributesMonthlyStatisticsAcrossCorrectMonths() {
        User agent = user(2L, UserRole.AGENT);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = LocalDateTime.of(now.getYear(), 1, 10, 10, 0);
        LocalDateTime resolved = now.withDayOfMonth(10).withHour(15).withMinute(0).withSecond(0).withNano(0);
        Ticket ticket = ticket(TicketStatus.CLOSED, agent, created);
        ticket.setResolvedAt(resolved);
        ticket.setClosedAt(resolved.plusHours(4));

        when(ticketRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(ticket));

        var statistics = service.getPeriodStatistics(StatisticsGranularity.MONTH);

        int currentMonthIndex = now.getMonthValue() - 1;
        assertEquals(1, statistics.buckets().getFirst().created());
        assertEquals(1, statistics.buckets().get(currentMonthIndex).resolved());
        assertEquals(1, statistics.buckets().get(currentMonthIndex).closed());
    }

    @Test
    void spansSeveralYearsInMonthlyStatistics() {
        User agent = user(2L, UserRole.AGENT);
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.from(now);
        YearMonth firstMonth = currentMonth.minusMonths(13);
        LocalDateTime created = firstMonth.atDay(10).atTime(10, 0);
        Ticket ticket = ticket(TicketStatus.CLOSED, agent, created);
        ticket.setResolvedAt(created.plusHours(20));
        ticket.setClosedAt(created.plusHours(24));

        when(ticketRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(ticket));

        var statistics = service.getPeriodStatistics(StatisticsGranularity.MONTH);

        int expectedBuckets = (int) ChronoUnit.MONTHS.between(firstMonth, currentMonth) + 1;
        assertEquals(expectedBuckets, statistics.buckets().size());
        assertEquals(1, statistics.buckets().getFirst().created());
        assertTrue(statistics.buckets().getFirst().label()
                .endsWith(String.format("%02d", firstMonth.getYear() % 100)));
    }

    @Test
    void anchorsYearStatisticsToFirstActivityYear() {
        User agent = user(2L, UserRole.AGENT);
        int currentYear = LocalDateTime.now().getYear();
        int firstYear = currentYear - 5;
        LocalDateTime created = LocalDateTime.of(firstYear, 6, 10, 10, 0);
        Ticket ticket = ticket(TicketStatus.CLOSED, agent, created);
        ticket.setResolvedAt(created.plusHours(20));
        ticket.setClosedAt(created.plusHours(24));

        when(ticketRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(ticket));

        var statistics = service.getPeriodStatistics(StatisticsGranularity.YEAR);

        assertEquals(6, statistics.buckets().size());
        assertEquals(String.valueOf(firstYear), statistics.buckets().getFirst().label());
        assertEquals(String.valueOf(currentYear), statistics.buckets().getLast().label());
        assertEquals(1, statistics.buckets().getFirst().created());
    }

    @Test
    void padsNewYearStatisticsWithFourFutureYears() {
        User agent = user(2L, UserRole.AGENT);
        int currentYear = LocalDateTime.now().getYear();
        LocalDateTime created = LocalDateTime.of(currentYear, 6, 10, 10, 0);
        Ticket ticket = ticket(TicketStatus.CLOSED, agent, created);
        ticket.setResolvedAt(created.plusHours(20));
        ticket.setClosedAt(created.plusHours(24));

        when(ticketRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(ticket));

        var statistics = service.getPeriodStatistics(StatisticsGranularity.YEAR);

        assertEquals(5, statistics.buckets().size());
        assertEquals(String.valueOf(currentYear), statistics.buckets().getFirst().label());
        assertEquals(String.valueOf(currentYear + 4), statistics.buckets().getLast().label());
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
