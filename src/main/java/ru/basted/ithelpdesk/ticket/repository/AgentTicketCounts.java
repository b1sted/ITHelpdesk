package ru.basted.ithelpdesk.ticket.repository;

public interface AgentTicketCounts {
    Long getAgentId();

    long getTotal();

    long getActive();

    long getResolved();

    long getClosed();
}
