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
        return assignee == null ? fallbackHours : assignee.getTimeLimitHours();
    }

    public static LocalDateTime dueAt(Ticket ticket, long fallbackHours) {
        return dueAtWithLimit(ticket, limitHours(ticket.getAssignee(), fallbackHours));
    }

    public static LocalDateTime dueAtWithLimit(Ticket ticket, long limitHours) {
        return ticket.getCreatedAt()
                .plusHours(limitHours)
                .plusHours(ticket.getExtensionHours());
    }

    public static boolean isOverdue(Ticket ticket, long fallbackHours, LocalDateTime now) {
        return isOverdueWithLimit(ticket, limitHours(ticket.getAssignee(), fallbackHours), now);
    }

    public static boolean isOverdueWithLimit(Ticket ticket, long limitHours, LocalDateTime now) {
        return OVERDUE_STATUSES.contains(ticket.getStatus())
                && now.isAfter(dueAtWithLimit(ticket, limitHours));
    }
}
