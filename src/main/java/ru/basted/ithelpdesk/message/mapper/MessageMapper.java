package ru.basted.ithelpdesk.message.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ru.basted.ithelpdesk.message.dto.MessageCreateRequest;
import ru.basted.ithelpdesk.message.entity.Message;

@Mapper
public interface MessageMapper {
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "ticket", ignore = true)
    @Mapping(target = "author", ignore = true)
    Message toEntity(MessageCreateRequest dto);
}
