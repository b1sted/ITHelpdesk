package ru.basted.ithelpdesk.message.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.basted.ithelpdesk.core.exception.ConflictException;
import ru.basted.ithelpdesk.core.model.BaseEntity;
import ru.basted.ithelpdesk.core.security.CurrentUser;
import ru.basted.ithelpdesk.department.entity.Department;
import ru.basted.ithelpdesk.message.dto.MessageCreateRequest;
import ru.basted.ithelpdesk.message.entity.Message;
import ru.basted.ithelpdesk.message.mapper.MessageMapper;
import ru.basted.ithelpdesk.message.repository.MessageRepository;
import ru.basted.ithelpdesk.ticket.entity.Ticket;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;
import ru.basted.ithelpdesk.ticket.repository.TicketRepository;
import ru.basted.ithelpdesk.user.entity.User;
import ru.basted.ithelpdesk.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {
    @Mock
    private MessageRepository repository;
    @Mock
    private MessageMapper mapper;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private CurrentUser currentUser;

    private MessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MessageServiceImpl(repository, mapper, ticketRepository, currentUser);
    }

    @Test
    void agentCannotPostToTicketOwnedByAnotherAgent() {
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS, user(1L, UserRole.EMPLOYEE),
                user(2L, UserRole.AGENT));
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(currentUser.get()).thenReturn(user(3L, UserRole.AGENT));

        assertThrows(
                ConflictException.class,
                () -> service.addMessage(1L, new MessageCreateRequest("Текст"))
        );
    }

    @Test
    void administratorCannotPostToTicket() {
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS, user(1L, UserRole.EMPLOYEE),
                user(2L, UserRole.AGENT));
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(currentUser.get()).thenReturn(user(3L, UserRole.ADMIN));

        assertThrows(
                ConflictException.class,
                () -> service.addMessage(1L, new MessageCreateRequest("Текст"))
        );
    }

    @Test
    void employeeCannotPostToForeignTicket() {
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS, user(1L, UserRole.EMPLOYEE),
                user(2L, UserRole.AGENT));
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(currentUser.get()).thenReturn(user(3L, UserRole.EMPLOYEE));

        assertThrows(
                ConflictException.class,
                () -> service.addMessage(1L, new MessageCreateRequest("Текст"))
        );
    }

    @Test
    void authorReplyMovesWaitingTicketToAnswered() {
        User author = user(1L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(TicketStatus.WAITING_USER, author, user(2L, UserRole.AGENT));
        Message message = new Message();
        setId(message, 5L);
        message.setText("Уточняю детали");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(currentUser.get()).thenReturn(author);
        when(mapper.toEntity(any(MessageCreateRequest.class))).thenReturn(message);
        when(repository.save(message)).thenReturn(message);

        var response = service.addMessage(1L, new MessageCreateRequest("Уточняю детали"));

        assertEquals(TicketStatus.ANSWERED, ticket.getStatus());
        assertEquals("Уточняю детали", response.text());
    }

    private Ticket ticket(TicketStatus status, User creator, User assignee) {
        Ticket ticket = new Ticket();
        setId(ticket, 1L);
        ticket.setTitle("Заявка");
        ticket.setDescription("Описание");
        ticket.setStatus(status);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setCreator(creator);
        ticket.setAssignee(assignee);
        return ticket;
    }

    private User user(Long id, UserRole role) {
        User user = new User();
        setId(user, id);
        user.setFullName("Пользователь " + id);
        user.setEmail("user" + id + "@company.ru");
        user.setRole(role);
        user.setDepartment(department());
        return user;
    }

    private Department department() {
        Department department = new Department();
        setId(department, 1L);
        department.setName("IT Service Desk");
        return department;
    }

    private void setId(BaseEntity entity, Long id) {
        try {
            Field field = BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
