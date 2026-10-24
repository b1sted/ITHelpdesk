package ru.basted.ithelpdesk.statistics.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.basted.ithelpdesk.statistics.dto.AgentStatisticsResponse;
import ru.basted.ithelpdesk.statistics.dto.PeriodBucketResponse;
import ru.basted.ithelpdesk.statistics.dto.PeriodStatisticsResponse;
import ru.basted.ithelpdesk.statistics.dto.StatisticsGranularity;
import ru.basted.ithelpdesk.ticket.entity.Ticket;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;
import ru.basted.ithelpdesk.ticket.repository.TicketRepository;
import ru.basted.ithelpdesk.ticket.service.AgentCapacityPolicy;
import ru.basted.ithelpdesk.ticket.service.TicketSlaPolicy;
import ru.basted.ithelpdesk.user.entity.User;
import ru.basted.ithelpdesk.user.entity.UserRole;
import ru.basted.ithelpdesk.user.repository.UserRepository;

@Service
public class StatisticsServiceImpl implements StatisticsService {
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final long overdueHours;

    public StatisticsServiceImpl(
            UserRepository userRepository,
            TicketRepository ticketRepository,
            @Value("${app.ticket.overdue-hours:24}") long overdueHours
    ) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.overdueHours = overdueHours;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentStatisticsResponse> getAgentStatistics() {
        Map<Long, List<Ticket>> ticketsByAgent = ticketRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(ticket -> ticket.getAssignee() != null)
                .collect(Collectors.groupingBy(ticket -> ticket.getAssignee().getId()));

        LocalDateTime now = LocalDateTime.now();

        return userRepository.findAllByRoleOrderByFullNameAsc(UserRole.AGENT)
                .stream()
                .map(agent -> toResponse(agent, ticketsByAgent.getOrDefault(agent.getId(), List.of()), now))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PeriodStatisticsResponse getPeriodStatistics(StatisticsGranularity granularity) {
        List<Ticket> tickets = ticketRepository.findAllByOrderByCreatedAtDesc();
        LocalDateTime now = LocalDateTime.now();

        return granularity == StatisticsGranularity.YEAR
                ? buildYearStatistics(tickets, now)
                : buildMonthStatistics(tickets, now);
    }

    private PeriodStatisticsResponse buildMonthStatistics(List<Ticket> tickets, LocalDateTime now) {
        int year = now.getYear();
        Locale russian = Locale.forLanguageTag("ru");
        Bucket[] buckets = new Bucket[12];

        for (int month = 1; month <= 12; month++) {
            String label = Month.of(month).getDisplayName(TextStyle.SHORT, russian);
            buckets[month - 1] = new Bucket(label);
        }

        LocalDateTime from = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(year + 1, 1, 1, 0, 0);

        for (Ticket ticket : tickets) {
            addToBucket(buckets, ticket.getCreatedAt(), year, year, Bucket::markCreated);
            addToBucket(buckets, ticket.getResolvedAt(), year, year, Bucket::markResolved);
            addToBucket(buckets, ticket.getClosedAt(), year, year, Bucket::markClosed);
        }

        return toPeriodResponse(StatisticsGranularity.MONTH, buckets, tickets, from, to);
    }

    private PeriodStatisticsResponse buildYearStatistics(List<Ticket> tickets, LocalDateTime now) {
        int currentYear = now.getYear();
        int startYear = currentYear - 4;
        Bucket[] buckets = new Bucket[5];

        for (int index = 0; index < buckets.length; index++) {
            buckets[index] = new Bucket(String.valueOf(startYear + index));
        }

        LocalDateTime from = LocalDateTime.of(startYear, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(currentYear + 1, 1, 1, 0, 0);

        for (Ticket ticket : tickets) {
            addToBucket(buckets, ticket.getCreatedAt(), startYear, currentYear, Bucket::markCreated);
            addToBucket(buckets, ticket.getResolvedAt(), startYear, currentYear, Bucket::markResolved);
            addToBucket(buckets, ticket.getClosedAt(), startYear, currentYear, Bucket::markClosed);
        }

        return toPeriodResponse(StatisticsGranularity.YEAR, buckets, tickets, from, to);
    }

    private void addToBucket(
            Bucket[] buckets,
            LocalDateTime date,
            int startYear,
            int endYear,
            BucketAction action
    ) {
        if (date == null || date.getYear() < startYear || date.getYear() > endYear) {
            return;
        }
        action.apply(buckets[date.getYear() - startYear], date);
    }

    private PeriodStatisticsResponse toPeriodResponse(
            StatisticsGranularity granularity,
            Bucket[] buckets,
            List<Ticket> tickets,
            LocalDateTime from,
            LocalDateTime to
    ) {
        List<PeriodBucketResponse> responses = java.util.Arrays.stream(buckets)
                .map(Bucket::toResponse)
                .toList();

        long totalCreated = responses.stream().mapToLong(PeriodBucketResponse::created).sum();
        long totalResolved = responses.stream().mapToLong(PeriodBucketResponse::resolved).sum();
        long totalClosed = responses.stream().mapToLong(PeriodBucketResponse::closed).sum();

        double averageResolutionHours = tickets.stream()
                .filter(ticket -> ticket.getResolvedAt() != null)
                .filter(ticket -> !ticket.getResolvedAt().isBefore(from)
                        && ticket.getResolvedAt().isBefore(to))
                .mapToLong(ticket -> Duration.between(
                        ticket.getCreatedAt(), ticket.getResolvedAt()
                ).toMinutes())
                .average()
                .stream()
                .map(minutes -> Math.round(minutes / 60.0 * 10.0) / 10.0)
                .findFirst()
                .orElse(0.0);

        return new PeriodStatisticsResponse(
                granularity,
                responses,
                totalCreated,
                totalResolved,
                totalClosed,
                averageResolutionHours
        );
    }

    private AgentStatisticsResponse toResponse(User agent, List<Ticket> tickets, LocalDateTime now) {
        long active = tickets.stream()
                .filter(ticket -> AgentCapacityPolicy.ACTIVE_STATUSES.contains(ticket.getStatus()))
                .count();
        long resolved = tickets.stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.RESOLVED)
                .count();
        long closed = tickets.stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.CLOSED)
                .count();
        long overdue = tickets.stream()
                .filter(ticket -> TicketSlaPolicy.isOverdue(ticket, overdueHours, now))
                .count();

        return new AgentStatisticsResponse(
                agent.getId(),
                agent.getFullName(),
                agent.getEmail(),
                agent.getDepartment().getName(),
                agent.getTicketLimit(),
                agent.getTimeLimitHours(),
                tickets.size(),
                active,
                resolved,
                closed,
                overdue
        );
    }

    @FunctionalInterface
    private interface BucketAction {
        void apply(Bucket bucket, LocalDateTime date);
    }

    private static final class Bucket {
        private final String label;
        private long created;
        private long resolved;
        private long closed;

        private Bucket(String label) {
            this.label = label;
        }

        private void markCreated(LocalDateTime date) {
            created++;
        }

        private void markResolved(LocalDateTime date) {
            resolved++;
        }

        private void markClosed(LocalDateTime date) {
            closed++;
        }

        private PeriodBucketResponse toResponse() {
            return new PeriodBucketResponse(label, created, resolved, closed);
        }
    }
}
