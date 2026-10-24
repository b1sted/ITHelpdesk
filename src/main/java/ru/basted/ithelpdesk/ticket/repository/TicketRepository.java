package ru.basted.ithelpdesk.ticket.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @EntityGraph(attributePaths = {"creator", "assignee", "category",
            "creator.department", "assignee.department"})
    List<Ticket> findAllByAssigneeIdOrStatusOrderByCreatedAtDesc(Long assigneeId, TicketStatus status);

    @EntityGraph(attributePaths = {"creator", "assignee", "category",
            "creator.department", "assignee.department"})
    List<Ticket> findAllByAssigneeIdAndStatusOrderByCreatedAtDesc(Long assigneeId, TicketStatus status);

    long countByAssigneeIdAndStatusIn(Long assigneeId, Collection<TicketStatus> statuses);

    List<Ticket> findAllByStatusAndResolvedAtLessThanEqualOrderByCreatedAtDesc(
            TicketStatus status,
            LocalDateTime resolvedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE tickets
            SET status = 'IN_PROGRESS',
                assignee_id = :agentId,
                version = version + 1
            WHERE id = :ticketId
              AND status = 'NEW'
              AND (SELECT count(*)
                   FROM tickets active
                   WHERE active.assignee_id = :agentId
                     AND active.status IN ('IN_PROGRESS', 'WAITING_USER', 'ANSWERED')) < :limit
            """, nativeQuery = true)
    int claimIfCapacityAvailable(
            @Param("ticketId") Long ticketId,
            @Param("agentId") Long agentId,
            @Param("limit") int limit
    );
}
