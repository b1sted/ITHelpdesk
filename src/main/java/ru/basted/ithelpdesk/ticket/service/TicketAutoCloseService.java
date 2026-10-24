package ru.basted.ithelpdesk.ticket.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.basted.ithelpdesk.settings.service.SettingsService;
import ru.basted.ithelpdesk.ticket.entity.Ticket;
import ru.basted.ithelpdesk.ticket.entity.TicketStatus;
import ru.basted.ithelpdesk.ticket.repository.TicketRepository;

@Service
public class TicketAutoCloseService {
    private final TicketRepository ticketRepository;
    private final SettingsService settingsService;

    public TicketAutoCloseService(
            TicketRepository ticketRepository,
            SettingsService settingsService
    ) {
        this.ticketRepository = ticketRepository;
        this.settingsService = settingsService;
    }

    @Scheduled(
            initialDelayString = "${app.ticket.auto-close-check-ms:3600000}",
            fixedDelayString = "${app.ticket.auto-close-check-ms:3600000}"
    )
    @Transactional
    public void closeResolvedTickets() {
        int autoCloseHours = settingsService.getAutoCloseResolvedHours();
        if (autoCloseHours <= 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusHours(autoCloseHours);
        List<Ticket> resolved = ticketRepository.findAllByStatusOrderByCreatedAtDesc(
                TicketStatus.RESOLVED
        );

        for (Ticket ticket : resolved) {
            LocalDateTime resolvedAt = ticket.getResolvedAt() == null
                    ? ticket.getCreatedAt()
                    : ticket.getResolvedAt();
            if (!resolvedAt.isAfter(cutoff)) {
                ticket.setStatus(TicketStatus.CLOSED);
                ticket.setClosedAt(now);
                ticketRepository.save(ticket);
            }
        }
    }
}
