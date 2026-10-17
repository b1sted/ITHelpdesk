package ru.basted.ithelpdesk.ticket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.basted.ithelpdesk.ticket.entity.Ticket;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findAllByOrderByCreatedAtDesc();

    List<Ticket> findAllByStatusOrderByCreatedAtDesc(TicketStatus status);

    List<Ticket> findAllByCreatorIdOrderByCreatedAtDesc(Long creatorId);

    List<Ticket> findAllByAssigneeIdOrderByCreatedAtDesc(Long assigneeId);
}