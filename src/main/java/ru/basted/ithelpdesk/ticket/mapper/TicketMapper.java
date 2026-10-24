package ru.basted.ithelpdesk.ticket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ru.basted.ithelpdesk.ticket.dto.TicketCreateRequest;
import ru.basted.ithelpdesk.ticket.entity.Ticket;

@Mapper
public interface TicketMapper {
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "category", ignore = true)
    Ticket toEntity(TicketCreateRequest dto);
}