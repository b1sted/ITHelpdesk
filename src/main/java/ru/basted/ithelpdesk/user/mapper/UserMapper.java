package ru.basted.ithelpdesk.user.mapper;

import org.mapstruct.Mapper;

import ru.basted.ithelpdesk.user.dto.UserCreateRequest;
import ru.basted.ithelpdesk.user.entity.User;

@Mapper
public interface UserMapper {
    User toEntity(UserCreateRequest dto);
}