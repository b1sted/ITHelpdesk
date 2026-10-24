package ru.basted.ithelpdesk.statistics.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.basted.ithelpdesk.statistics.dto.AgentStatisticsResponse;
import ru.basted.ithelpdesk.statistics.dto.PeriodBucketResponse;
import ru.basted.ithelpdesk.statistics.dto.PeriodStatisticsResponse;
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

@Service
public class StatisticsServiceImpl implements StatisticsService {
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public StatisticsServiceImpl(
            UserRepository userRepository,
            TicketRepository ticketRepository
    ) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentStatisticsResponse> getAgentStatistics() {
        Map<Long, AgentTicketCounts> countsByAgent = ticketRepository.countTicketsByAssignee(
                        AgentCapacityPolicy.ACTIVE_STATUSES,
                        TicketStatus.RESOLVED,
                        TicketStatus.CLOSED
                )
                .stream()
                .collect(Collectors.toMap(AgentTicketCounts::getAgentId, counts -> counts));

        Map<Long, Long> overdueByAgent = ticketRepository
                .countOverdueTicketsByAssignee(LocalDateTime.now())
                .stream()
                .collect(Collectors.toMap(AgentOverdueCounts::getAgentId, AgentOverdueCounts::getOverdue));

        return userRepository.findAllByRoleOrderByFullNameAsc(UserRole.AGENT)
                .stream()
                .map(agent -> toResponse(
                        agent,
                        countsByAgent.get(agent.getId()),
                        overdueByAgent.getOrDefault(agent.getId(), 0L)
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PeriodStatisticsResponse getPeriodStatistics(StatisticsGranularity granularity) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstCreatedAt = ticketRepository.findFirstCreatedAt();

        return granularity == StatisticsGranularity.YEAR
                ? buildYearStatistics(firstCreatedAt, now)
                : buildMonthStatistics(firstCreatedAt, now);
    }

    private PeriodStatisticsResponse buildMonthStatistics(
            LocalDateTime firstCreatedAt,
            LocalDateTime now
    ) {
        YearMonth currentMonth = YearMonth.from(now);
        YearMonth firstMonth = firstCreatedAt == null
                ? YearMonth.of(now.getYear(), 1)
                : YearMonth.from(firstCreatedAt);
        YearMonth startMonth = firstMonth.isAfter(currentMonth) ? currentMonth : firstMonth;
        YearMonth endMonth = firstCreatedAt == null
                ? YearMonth.of(now.getYear(), 12)
                : currentMonth;
        boolean multiYear = startMonth.getYear() != endMonth.getYear();
        Locale russian = Locale.forLanguageTag("ru");
        int bucketCount = (int) ChronoUnit.MONTHS.between(startMonth, endMonth) + 1;

        Map<YearMonth, Long> created = toMonthCounts(ticketRepository.countCreatedByPeriod("month"));
        Map<YearMonth, Long> resolved = toMonthCounts(ticketRepository.countResolvedByPeriod("month"));
        Map<YearMonth, Long> closed = toMonthCounts(ticketRepository.countClosedByPeriod("month"));

        List<PeriodBucketResponse> responses = new ArrayList<>(bucketCount);
        for (int index = 0; index < bucketCount; index++) {
            YearMonth month = startMonth.plusMonths(index);
            String label = month.getMonth().getDisplayName(TextStyle.SHORT, russian);
            if (multiYear) {
                label = label + " " + String.format("%02d", month.getYear() % 100);
            }
            responses.add(new PeriodBucketResponse(
                    label,
                    created.getOrDefault(month, 0L),
                    resolved.getOrDefault(month, 0L),
                    closed.getOrDefault(month, 0L)
            ));
        }

        LocalDateTime from = startMonth.atDay(1).atStartOfDay();
        LocalDateTime to = endMonth.plusMonths(1).atDay(1).atStartOfDay();

        return toPeriodResponse(StatisticsGranularity.MONTH, responses, from, to);
    }

    private PeriodStatisticsResponse buildYearStatistics(
            LocalDateTime firstCreatedAt,
            LocalDateTime now
    ) {
        int currentYear = now.getYear();
        int firstYear = firstCreatedAt == null
                ? currentYear
                : Math.min(firstCreatedAt.getYear(), currentYear);
        int endYear = Math.max(currentYear, firstYear + 4);

        Map<Integer, Long> created = toYearCounts(ticketRepository.countCreatedByPeriod("year"));
        Map<Integer, Long> resolved = toYearCounts(ticketRepository.countResolvedByPeriod("year"));
        Map<Integer, Long> closed = toYearCounts(ticketRepository.countClosedByPeriod("year"));

        List<PeriodBucketResponse> responses = new ArrayList<>(endYear - firstYear + 1);
        for (int year = firstYear; year <= endYear; year++) {
            responses.add(new PeriodBucketResponse(
                    String.valueOf(year),
                    created.getOrDefault(year, 0L),
                    resolved.getOrDefault(year, 0L),
                    closed.getOrDefault(year, 0L)
            ));
        }

        LocalDateTime from = LocalDateTime.of(firstYear, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(endYear + 1, 1, 1, 0, 0);

        return toPeriodResponse(StatisticsGranularity.YEAR, responses, from, to);
    }

    private Map<YearMonth, Long> toMonthCounts(List<PeriodTicketCounts> counts) {
        return counts.stream()
                .collect(Collectors.toMap(
                        count -> YearMonth.from(count.getBucket()),
                        PeriodTicketCounts::getTotal,
                        Long::sum
                ));
    }

    private Map<Integer, Long> toYearCounts(List<PeriodTicketCounts> counts) {
        return counts.stream()
                .collect(Collectors.toMap(
                        count -> count.getBucket().getYear(),
                        PeriodTicketCounts::getTotal,
                        Long::sum
                ));
    }

    private PeriodStatisticsResponse toPeriodResponse(
            StatisticsGranularity granularity,
            List<PeriodBucketResponse> buckets,
            LocalDateTime from,
            LocalDateTime to
    ) {
        long totalCreated = buckets.stream().mapToLong(PeriodBucketResponse::created).sum();
        long totalResolved = buckets.stream().mapToLong(PeriodBucketResponse::resolved).sum();
        long totalClosed = buckets.stream().mapToLong(PeriodBucketResponse::closed).sum();

        Double average = ticketRepository.averageResolutionHours(from, to);
        double averageResolutionHours = average == null
                ? 0.0
                : Math.round(average * 10.0) / 10.0;

        return new PeriodStatisticsResponse(
                granularity,
                buckets,
                totalCreated,
                totalResolved,
                totalClosed,
                averageResolutionHours
        );
    }

    private AgentStatisticsResponse toResponse(
            User agent,
            AgentTicketCounts counts,
            long overdue
    ) {
        long total = counts == null ? 0 : counts.getTotal();
        long active = counts == null ? 0 : counts.getActive();
        long resolved = counts == null ? 0 : counts.getResolved();
        long closed = counts == null ? 0 : counts.getClosed();

        return new AgentStatisticsResponse(
                agent.getId(),
                agent.getFullName(),
                agent.getEmail(),
                agent.getDepartment().getName(),
                agent.getTicketLimit(),
                agent.getTimeLimitHours(),
                total,
                active,
                resolved,
                closed,
                overdue
        );
    }
}
