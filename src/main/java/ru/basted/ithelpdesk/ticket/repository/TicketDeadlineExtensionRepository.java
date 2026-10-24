package ru.basted.ithelpdesk.ticket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ru.basted.ithelpdesk.ticket.entity.TicketDeadlineExtension;

public interface TicketDeadlineExtensionRepository
        extends JpaRepository<TicketDeadlineExtension, Long> {
    @EntityGraph(attributePaths = {"createdBy", "createdBy.department"})
    List<TicketDeadlineExtension> findAllByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
