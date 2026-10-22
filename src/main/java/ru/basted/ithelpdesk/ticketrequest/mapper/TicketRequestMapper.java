package ru.basted.ithelpdesk.ticketrequest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ru.basted.ithelpdesk.ticketrequest.dto.TicketRequestCreateRequest;
import ru.basted.ithelpdesk.ticketrequest.entity.TicketRequest;

@Mapper
public interface TicketRequestMapper {
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "decisionComment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "decidedAt", ignore = true)
    @Mapping(target = "ticket", ignore = true)
    @Mapping(target = "requester", ignore = true)
    @Mapping(target = "targetAssignee", ignore = true)
    @Mapping(target = "decidedBy", ignore = true)
    TicketRequest toEntity(TicketRequestCreateRequest dto);
}
