package ru.basted.ithelpdesk.departments.mapper;

import org.mapstruct.Mapper;

import ru.basted.ithelpdesk.departments.dto.DepartmentsCreateRequest;
import ru.basted.ithelpdesk.departments.dto.DepartmentsResponse;
import ru.basted.ithelpdesk.departments.entity.Departments;

@Mapper
public interface DepartmentsMapper {
    Departments toEntity(DepartmentsCreateRequest dto);
    DepartmentsResponse toResponseDto(Departments departments);
}
