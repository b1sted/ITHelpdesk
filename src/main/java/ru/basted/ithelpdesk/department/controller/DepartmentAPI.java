package ru.basted.ithelpdesk.department.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.basted.ithelpdesk.department.dto.DepartmentChangeName;
import ru.basted.ithelpdesk.department.dto.DepartmentCreateRequest;
import ru.basted.ithelpdesk.department.dto.DepartmentResponse;

@Tag(name = "Департаменты", description = "Управление базой данных департаментами")
public interface DepartmentAPI {
    @Operation(
            summary = "Получить список департаментов, зарегистрированных в базе данных",
            description = "Возвращает список всех зарегистрированных департаментов с информацией о них."
    )
    @GetMapping
    ResponseEntity<List<DepartmentResponse>> getAllDepartments();

    @Operation(
            summary = "Получить информацию о департаменте по его уникальному идентификатору",
            description = "Возвращает информацию о конкретном департаменте, если он зарегистрирован в базе данных, " +
                    "по его уникальному идентификатору. Доступна всем авторизованным пользователям"
    )
    @GetMapping("/{id}")
    ResponseEntity<DepartmentResponse> getDepartmentById(
            @Parameter(description = "Уникальный идентификатор департамента", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "ID не может быть меньше нуля") Long id
    );

    @Operation(
            summary = "Создать новый департамент",
            description = "Создание записи в базе данных о новом департаменте. Доступно только администраторам"
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody DepartmentCreateRequest createRequest
    );

    @Operation(
            summary = "Смена имени департамента",
            description = "Изменение имени существующего департамента. Доступна только администраторам."
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> updateName(
            @Parameter(description = "Уникальный идентификатор департамента", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "ID не может быть меньше нуля") Long id,

            @Valid @RequestBody DepartmentChangeName changeName
    );

    @Operation(
            summary = "Удаление департамента",
            description = "Удаление департамента. Доступна только администраторам."
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteDepartment(
            @Parameter(description = "Уникальный идентификатор департамента", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "ID не может быть меньше нуля") Long id
    );
}