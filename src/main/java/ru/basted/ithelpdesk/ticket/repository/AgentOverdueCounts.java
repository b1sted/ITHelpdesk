package ru.basted.ithelpdesk.ticket.repository;

public interface AgentOverdueCounts {
    Long getAgentId();

    long getOverdue();
}
