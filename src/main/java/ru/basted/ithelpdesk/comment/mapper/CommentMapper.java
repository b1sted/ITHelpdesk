package ru.basted.ithelpdesk.comment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ru.basted.ithelpdesk.comment.dto.CommentCreateRequest;
import ru.basted.ithelpdesk.comment.entity.Comment;

@Mapper
public interface CommentMapper {
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "ticketId", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    Comment toEntity(CommentCreateRequest dto);
}