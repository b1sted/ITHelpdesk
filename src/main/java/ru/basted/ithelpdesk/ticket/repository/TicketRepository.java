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

    @Query("""
            SELECT t.assignee.id AS agentId,
                   count(t) AS total,
                   sum(CASE WHEN t.status IN :activeStatuses THEN 1 ELSE 0 END) AS active,
                   sum(CASE WHEN t.status = :resolvedStatus THEN 1 ELSE 0 END) AS resolved,
                   sum(CASE WHEN t.status = :closedStatus THEN 1 ELSE 0 END) AS closed
            FROM Ticket t
            WHERE t.assignee IS NOT NULL
            GROUP BY t.assignee.id
            """)
    List<AgentTicketCounts> countTicketsByAssignee(
            @Param("activeStatuses") Collection<TicketStatus> activeStatuses,
            @Param("resolvedStatus") TicketStatus resolvedStatus,
            @Param("closedStatus") TicketStatus closedStatus
    );

    @Query(value = """
            SELECT t.assignee_id AS agentId, count(*) AS overdue
            FROM tickets t
            JOIN users u ON u.id = t.assignee_id
            WHERE t.status IN ('NEW', 'IN_PROGRESS', 'ANSWERED')
              AND :now > t.created_at
                  + ((u.time_limit_hours + t.extension_hours) * interval '1 hour')
            GROUP BY t.assignee_id
            """, nativeQuery = true)
    List<AgentOverdueCounts> countOverdueTicketsByAssignee(@Param("now") LocalDateTime now);

    @Query("SELECT min(t.createdAt) FROM Ticket t")
    LocalDateTime findFirstCreatedAt();

    @Query(value = """
            SELECT date_trunc(:unit, t.created_at) AS bucket, count(*) AS total
            FROM tickets t
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<PeriodTicketCounts> countCreatedByPeriod(@Param("unit") String unit);

    @Query(value = """
            SELECT date_trunc(:unit, t.resolved_at) AS bucket, count(*) AS total
            FROM tickets t
            WHERE t.resolved_at IS NOT NULL
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<PeriodTicketCounts> countResolvedByPeriod(@Param("unit") String unit);

    @Query(value = """
            SELECT date_trunc(:unit, t.closed_at) AS bucket, count(*) AS total
            FROM tickets t
            WHERE t.closed_at IS NOT NULL
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<PeriodTicketCounts> countClosedByPeriod(@Param("unit") String unit);

    @Query(value = """
            SELECT avg(extract(epoch FROM (t.resolved_at - t.created_at)) / 3600.0)
            FROM tickets t
            WHERE t.resolved_at >= :from AND t.resolved_at < :to
            """, nativeQuery = true)
    Double averageResolutionHours(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
