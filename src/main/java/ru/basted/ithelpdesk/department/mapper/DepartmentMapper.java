package ru.basted.ithelpdesk.department.mapper;

import org.mapstruct.Mapper;

import ru.basted.ithelpdesk.department.dto.DepartmentCreateRequest;
import ru.basted.ithelpdesk.department.dto.DepartmentResponse;
import ru.basted.ithelpdesk.department.entity.Department;

@Mapper
public interface DepartmentMapper {
    Department toEntity(DepartmentCreateRequest dto);
    DepartmentResponse toResponseDto(Department department);
}