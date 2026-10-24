package ru.basted.ithelpdesk.ticket.repository;

import java.time.LocalDateTime;

public interface PeriodTicketCounts {
    LocalDateTime getBucket();

    long getTotal();
}
