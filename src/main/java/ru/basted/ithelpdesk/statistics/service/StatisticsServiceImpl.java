package ru.basted.ithelpdesk.statistics.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.basted.ithelpdesk.statistics.dto.AgentStatisticsResponse;
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
}
