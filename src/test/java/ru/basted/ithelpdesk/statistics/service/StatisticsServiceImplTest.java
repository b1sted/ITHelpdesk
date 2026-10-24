package ru.basted.ithelpdesk.statistics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import ru.basted.ithelpdesk.core.model.BaseEntity;
import ru.basted.ithelpdesk.department.entity.Department;
import ru.basted.ithelpdesk.statistics.dto.StatisticsGranularity;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;
import ru.basted.ithelpdesk.ticket.repository.AgentOverdueCounts;
import ru.basted.ithelpdesk.ticket.repository.AgentTicketCounts;
import ru.basted.ithelpdesk.ticket.repository.PeriodTicketCounts;
import ru.basted.ithelpdesk.ticket.repository.TicketRepository;
import ru.basted.ithelpdesk.ticket.service.AgentCapacityPolicy;
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
        service = new StatisticsServiceImpl(userRepository, ticketRepository);
    }

    @Test
    void aggregatesAgentTicketsAndOverdueCount() {
        User agent = user(2L, UserRole.AGENT);
        agent.setTicketLimit(5);
        agent.setTimeLimitHours(12);

        when(userRepository.findAllByRoleOrderByFullNameAsc(UserRole.AGENT))
                .thenReturn(List.of(agent));
        when(ticketRepository.countTicketsByAssignee(
                AgentCapacityPolicy.ACTIVE_STATUSES, TicketStatus.RESOLVED, TicketStatus.CLOSED
        )).thenReturn(List.of(agentCounts(2L, 4, 2, 1, 1)));
        when(ticketRepository.countOverdueTicketsByAssignee(any(LocalDateTime.class)))
                .thenReturn(List.of(overdueCounts(2L, 1)));

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
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = LocalDateTime.of(now.getYear(), 1, 10, 10, 0);
        LocalDateTime resolved = created.plusHours(20);
        LocalDateTime closed = created.plusHours(24);
        stubPeriodStatistics(created, resolved, closed, 20.0, "month");

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
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = LocalDateTime.of(now.getYear(), 1, 10, 10, 0);
        LocalDateTime resolved = now.withDayOfMonth(10).withHour(15).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime closed = resolved.plusHours(4);
        stubPeriodStatistics(created, resolved, closed, 20.0, "month");

        var statistics = service.getPeriodStatistics(StatisticsGranularity.MONTH);

        int currentMonthIndex = now.getMonthValue() - 1;
        assertEquals(1, statistics.buckets().getFirst().created());
        assertEquals(1, statistics.buckets().get(currentMonthIndex).resolved());
        assertEquals(1, statistics.buckets().get(currentMonthIndex).closed());
    }

    @Test
    void spansSeveralYearsInMonthlyStatistics() {
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.from(now);
        YearMonth firstMonth = currentMonth.minusMonths(13);
        LocalDateTime created = firstMonth.atDay(10).atTime(10, 0);
        LocalDateTime resolved = created.plusHours(20);
        LocalDateTime closed = created.plusHours(24);
        stubPeriodStatistics(created, resolved, closed, 20.0, "month");

        var statistics = service.getPeriodStatistics(StatisticsGranularity.MONTH);

        int expectedBuckets = (int) ChronoUnit.MONTHS.between(firstMonth, currentMonth) + 1;
        assertEquals(expectedBuckets, statistics.buckets().size());
        assertEquals(1, statistics.buckets().getFirst().created());
        assertTrue(statistics.buckets().getFirst().label()
                .endsWith(String.format("%02d", firstMonth.getYear() % 100)));
    }

    @Test
    void anchorsYearStatisticsToFirstActivityYear() {
        int currentYear = LocalDateTime.now().getYear();
        int firstYear = currentYear - 5;
        LocalDateTime created = LocalDateTime.of(firstYear, 6, 10, 10, 0);
        LocalDateTime resolved = created.plusHours(20);
        LocalDateTime closed = created.plusHours(24);
        stubPeriodStatistics(created, resolved, closed, 20.0, "year");

        var statistics = service.getPeriodStatistics(StatisticsGranularity.YEAR);

        assertEquals(6, statistics.buckets().size());
        assertEquals(String.valueOf(firstYear), statistics.buckets().getFirst().label());
        assertEquals(String.valueOf(currentYear), statistics.buckets().getLast().label());
        assertEquals(1, statistics.buckets().getFirst().created());
    }

    @Test
    void padsNewYearStatisticsWithFourFutureYears() {
        int currentYear = LocalDateTime.now().getYear();
        LocalDateTime created = LocalDateTime.of(currentYear, 6, 10, 10, 0);
        LocalDateTime resolved = created.plusHours(20);
        LocalDateTime closed = created.plusHours(24);
        stubPeriodStatistics(created, resolved, closed, 20.0, "year");

        var statistics = service.getPeriodStatistics(StatisticsGranularity.YEAR);

        assertEquals(5, statistics.buckets().size());
        assertEquals(String.valueOf(currentYear), statistics.buckets().getFirst().label());
        assertEquals(String.valueOf(currentYear + 4), statistics.buckets().getLast().label());
    }

    @Test
    void returnsEmptyMonthlyStatisticsWhenThereAreNoTickets() {
        when(ticketRepository.findFirstCreatedAt()).thenReturn(null);
        when(ticketRepository.countCreatedByPeriod("month")).thenReturn(List.of());
        when(ticketRepository.countResolvedByPeriod("month")).thenReturn(List.of());
        when(ticketRepository.countClosedByPeriod("month")).thenReturn(List.of());
        when(ticketRepository.averageResolutionHours(any(), any())).thenReturn(null);

        var statistics = service.getPeriodStatistics(StatisticsGranularity.MONTH);

        assertEquals(12, statistics.buckets().size());
        assertEquals(0, statistics.totalCreated());
        assertEquals(0, statistics.totalResolved());
        assertEquals(0, statistics.totalClosed());
        assertEquals(0.0, statistics.averageResolutionHours());
    }

    private void stubPeriodStatistics(
            LocalDateTime created,
            LocalDateTime resolved,
            LocalDateTime closed,
            double averageHours,
            String unit
    ) {
        when(ticketRepository.findFirstCreatedAt()).thenReturn(created);
        when(ticketRepository.countCreatedByPeriod(unit))
                .thenReturn(List.of(periodCounts(created, 1)));
        when(ticketRepository.countResolvedByPeriod(unit))
                .thenReturn(List.of(periodCounts(resolved, 1)));
        when(ticketRepository.countClosedByPeriod(unit))
                .thenReturn(List.of(periodCounts(closed, 1)));
        when(ticketRepository.averageResolutionHours(any(), any())).thenReturn(averageHours);
    }

    private AgentTicketCounts agentCounts(
            Long agentId,
            long total,
            long active,
            long resolved,
            long closed
    ) {
        return new AgentTicketCounts() {
            @Override
            public Long getAgentId() {
                return agentId;
            }

            @Override
            public long getTotal() {
                return total;
            }

            @Override
            public long getActive() {
                return active;
            }

            @Override
            public long getResolved() {
                return resolved;
            }

            @Override
            public long getClosed() {
                return closed;
            }
        };
    }

    private AgentOverdueCounts overdueCounts(Long agentId, long overdue) {
        return new AgentOverdueCounts() {
            @Override
            public Long getAgentId() {
                return agentId;
            }

            @Override
            public long getOverdue() {
                return overdue;
            }
        };
    }

    private PeriodTicketCounts periodCounts(LocalDateTime bucket, long total) {
        return new PeriodTicketCounts() {
            @Override
            public LocalDateTime getBucket() {
                return bucket;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
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
