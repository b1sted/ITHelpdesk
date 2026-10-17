package ru.basted.ithelpdesk.comment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.basted.ithelpdesk.comment.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByTicketIdOrderByCreatedAtAsc(Long ticketId);
}