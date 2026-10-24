package ru.basted.ithelpdesk.ticketrequest.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ru.basted.ithelpdesk.ticketrequest.entity.TicketRequest;
import ru.basted.ithelpdesk.ticketrequest.entity.TicketRequestStatus;

public interface TicketRequestRepository extends JpaRepository<TicketRequest, Long> {
    @Override
    @EntityGraph(attributePaths = {"ticket", "ticket.assignee", "ticket.creator",
            "ticket.category", "ticket.assignee.department", "ticket.creator.department",
            "requester", "requester.department", "targetAssignee", "targetAssignee.department",
            "decidedBy", "decidedBy.department"})
    Optional<TicketRequest> findById(Long id);

    @EntityGraph(attributePaths = {"ticket", "requester", "requester.department",
            "targetAssignee", "targetAssignee.department", "decidedBy", "decidedBy.department"})
    List<TicketRequest> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"ticket", "requester", "requester.department",
            "targetAssignee", "targetAssignee.department", "decidedBy", "decidedBy.department"})
    List<TicketRequest> findAllByStatusOrderByCreatedAtDesc(TicketRequestStatus status);

    @EntityGraph(attributePaths = {"ticket", "requester", "requester.department",
            "targetAssignee", "targetAssignee.department", "decidedBy", "decidedBy.department"})
    List<TicketRequest> findAllByTicketIdOrderByCreatedAtAsc(Long ticketId);

    boolean existsByTicketIdAndStatus(Long ticketId, TicketRequestStatus status);
}
