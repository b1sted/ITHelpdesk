package ru.basted.ithelpdesk.ticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.basted.ithelpdesk.settings.service.SettingsService;
import ru.basted.ithelpdesk.ticket.entity.Ticket;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;
import ru.basted.ithelpdesk.ticket.repository.TicketRepository;

@ExtendWith(MockitoExtension.class)
class TicketAutoCloseServiceTest {
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private SettingsService settingsService;

    private TicketAutoCloseService service;

    @BeforeEach
    void setUp() {
        service = new TicketAutoCloseService(ticketRepository, settingsService);
    }

    @Test
    void closesResolvedTicketsPastDeadline() {
        Ticket expired = ticket(LocalDateTime.now().minusHours(30));
        Ticket recent = ticket(LocalDateTime.now().minusHours(1));
        when(settingsService.getAutoCloseResolvedHours()).thenReturn(24);
        when(ticketRepository.findAllByStatusOrderByCreatedAtDesc(TicketStatus.RESOLVED))
                .thenReturn(List.of(expired, recent));

        service.closeResolvedTickets();

        assertEquals(TicketStatus.CLOSED, expired.getStatus());
        assertNotNull(expired.getClosedAt());
        assertEquals(TicketStatus.RESOLVED, recent.getStatus());
        verify(ticketRepository).save(expired);
        verify(ticketRepository, never()).save(recent);
    }

    @Test
    void doesNothingWhenAutoCloseDisabled() {
        when(settingsService.getAutoCloseResolvedHours()).thenReturn(0);

        service.closeResolvedTickets();

        verifyNoInteractions(ticketRepository);
    }

    private Ticket ticket(LocalDateTime resolvedAt) {
        Ticket ticket = new Ticket();
        ticket.setTitle("Заявка");
        ticket.setDescription("Описание");
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setCreatedAt(resolvedAt.minusHours(1));
        ticket.setResolvedAt(resolvedAt);
        return ticket;
    }
}
