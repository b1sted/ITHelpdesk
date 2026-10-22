package ru.basted.ithelpdesk.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ru.basted.ithelpdesk.user.dto.UserCreateRequest;
import ru.basted.ithelpdesk.user.entity.User;

@Mapper
public interface UserMapper {
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "ticketLimit", ignore = true)
    @Mapping(target = "timeLimitHours", ignore = true)
    User toEntity(UserCreateRequest dto);
}