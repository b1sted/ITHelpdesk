package ru.basted.ithelpdesk.ticket.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ru.basted.ithelpdesk.ticket.entity.TicketClosure;

public interface TicketClosureRepository extends JpaRepository<TicketClosure, Long> {
    @EntityGraph(attributePaths = {"closedBy", "closedBy.department"})
    Optional<TicketClosure> findFirstByTicketIdOrderByCreatedAtDesc(Long ticketId);
}
