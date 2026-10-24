package ru.basted.ithelpdesk.ticket.service;

import java.time.LocalDateTime;
import java.util.Set;

import ru.basted.ithelpdesk.ticket.entity.Ticket;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;
import ru.basted.ithelpdesk.user.entity.User;

public final class TicketSlaPolicy {
    public static final Set<TicketStatus> OVERDUE_STATUSES = Set.of(
            TicketStatus.NEW,
            TicketStatus.IN_PROGRESS,
            TicketStatus.ANSWERED
    );

    private TicketSlaPolicy() {
    }

    public static long limitHours(User assignee, long fallbackHours) {
        return assignee == null || assignee.getTimeLimitHours() == null
                ? fallbackHours
                : assignee.getTimeLimitHours();
    }

    public static LocalDateTime dueAt(Ticket ticket, long fallbackHours) {
        return ticket.getCreatedAt().plusHours(limitHours(ticket.getAssignee(), fallbackHours));
    }

    public static boolean isOverdue(Ticket ticket, long fallbackHours, LocalDateTime now) {
        return OVERDUE_STATUSES.contains(ticket.getStatus())
                && now.isAfter(dueAt(ticket, fallbackHours));
    }
}
