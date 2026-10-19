package ru.basted.ithelpdesk.ticket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ru.basted.ithelpdesk.ticket.dto.TicketCreateRequest;
import ru.basted.ithelpdesk.ticket.entity.Ticket;

@Mapper
public interface TicketMapper {
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "assigneeId", ignore = true)
    Ticket toEntity(TicketCreateRequest dto);
}