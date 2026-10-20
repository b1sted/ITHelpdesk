package ru.basted.ithelpdesk.message.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.basted.ithelpdesk.message.entity.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findAllByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
