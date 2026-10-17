package ru.basted.ithelpdesk.user.controller;

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

import ru.basted.ithelpdesk.user.dto.UserCreateRequest;
import ru.basted.ithelpdesk.user.dto.UserResponse;
import ru.basted.ithelpdesk.user.dto.UserUpdateRequest;

@Tag(name = "Пользователи", description = "Управление пользователями и каталогом сотрудников")
public interface UserAPI {
    @Operation(
            summary = "Получить список всех пользователей",
            description = "Возвращает список всех зарегистрированных пользователей. Доступно только администраторам"
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<List<UserResponse>> getAllUsers();

    @Operation(
            summary = "Получить информацию о пользователе по идентификатору",
            description = "Возвращает информацию о пользователе по его уникальному идентификатору. " +
                    "Доступно администраторам или самому пользователю"
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#id)")
    ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "Уникальный идентификатор пользователя", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "ID не может быть меньше нуля") Long id
    );

    @Operation(
            summary = "Создать нового пользователя",
            description = "Регистрация нового пользователя в базе данных. Доступно только администраторам"
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest createRequest
    );

    @Operation(
            summary = "Обновить информацию о пользователе",
            description = "Обновление профиля, департамента или роли пользователя. Доступно только администраторам"
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "Уникальный идентификатор пользователя", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "ID не может быть меньше нуля") Long id,

            @Valid @RequestBody UserUpdateRequest updateRequest
    );

    @Operation(
            summary = "Удалить пользователя",
            description = "Удаление или деактивация пользователя. Доступно только администраторам"
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteUser(
            @Parameter(description = "Уникальный идентификатор пользователя", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "ID не может быть меньше нуля") Long id
    );
}