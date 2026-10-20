package ru.basted.ithelpdesk.ticket.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ru.basted.ithelpdesk.ticket.entity.Ticket;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    @Override
    @EntityGraph(attributePaths = {"creator", "assignee", "category",
            "creator.department", "assignee.department"})
    Optional<Ticket> findById(Long id);

    @EntityGraph(attributePaths = {"creator", "assignee", "category",
            "creator.department", "assignee.department"})
    List<Ticket> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"creator", "assignee", "category",
            "creator.department", "assignee.department"})
    List<Ticket> findAllByStatusOrderByCreatedAtDesc(TicketStatus status);

    @EntityGraph(attributePaths = {"creator", "assignee", "category",
            "creator.department", "assignee.department"})
    List<Ticket> findAllByCreatorIdOrderByCreatedAtDesc(Long creatorId);

    @EntityGraph(attributePaths = {"creator", "assignee", "category",
            "creator.department", "assignee.department"})
    List<Ticket> findAllByCreatorIdAndStatusOrderByCreatedAtDesc(Long creatorId, TicketStatus status);
}
