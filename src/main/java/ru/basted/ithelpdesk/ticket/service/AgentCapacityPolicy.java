package ru.basted.ithelpdesk.ticket.service;

import java.util.Set;

import org.springframework.stereotype.Component;

import ru.basted.ithelpdesk.core.exception.ConflictException;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;
import ru.basted.ithelpdesk.ticket.repository.TicketRepository;
import ru.basted.ithelpdesk.user.entity.User;

@Component
public class AgentCapacityPolicy {
    public static final Set<TicketStatus> ACTIVE_STATUSES =
            Set.of(TicketStatus.IN_PROGRESS, TicketStatus.WAITING_USER, TicketStatus.ANSWERED);

    private final TicketRepository ticketRepository;

    public AgentCapacityPolicy(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public void checkCanAssign(User agent) {
        int limit = resolveLimit(agent);
        long activeTickets = ticketRepository.countByAssigneeIdAndStatusIn(agent.getId(), ACTIVE_STATUSES);

        if (activeTickets >= limit) {
            throw new ConflictException(
                    "Достигнут предел активных заявок специалиста (%d). Освободите заявку или измените предел"
                            .formatted(limit)
            );
        }
    }

    public int resolveLimit(User agent) {
        return agent.getTicketLimit();
    }
}
