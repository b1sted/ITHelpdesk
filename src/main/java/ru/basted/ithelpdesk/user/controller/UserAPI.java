package ru.basted.ithelpdesk.user.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.basted.ithelpdesk.core.exception.ApiError;
import ru.basted.ithelpdesk.user.dto.UserCreateRequest;
import ru.basted.ithelpdesk.user.dto.UserResponse;
import ru.basted.ithelpdesk.user.dto.UserUpdateRequest;

@Tag(name = "Пользователи", description = "Управление пользователями и каталогом сотрудников")
public interface UserAPI {
    String USER_JSON = """
            {
              "id": 1,
              "fullName": "Иванов Иван Иванович",
              "email": "ivanov.ii@company.ru",
              "role": "EMPLOYEE",
              "departmentName": "Департамент тестирования программных продуктов"
            }""";
    String USER_LIST_JSON = "[" + USER_JSON + "]";
    String ERROR_401_JSON = "{\"message\":\"Требуется аутентификация\"}";
    String ERROR_403_JSON = "{\"message\":\"Недостаточно прав для выполнения запроса\"}";
    String ERROR_404_JSON = "{\"message\":\"Не найден пользователь с ID 99\"}";
    String ERROR_400_JSON = "{\"message\":\"Некорректный формат электронной почты\"}";
    String ERROR_409_JSON = "{\"message\":\"Пользователь с электронной почтой ivanov.ii@company.ru уже существует\"}";

    @Operation(
            summary = "Получить список всех пользователей",
            description = "Возвращает список всех зарегистрированных пользователей. Доступно только администраторам"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список пользователей",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(value = USER_LIST_JSON))),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_401_JSON))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав (требуется роль ADMIN)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_403_JSON)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<List<UserResponse>> getAllUsers();

    @Operation(
            summary = "Получить информацию о пользователе по идентификатору",
            description = "Возвращает информацию о пользователе по его уникальному идентификатору. " +
                    "Доступно администраторам или самому пользователю"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Информация о пользователе",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(value = USER_JSON))),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_401_JSON))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав (требуется роль ADMIN)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_403_JSON))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_404_JSON)))
    })
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
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Пользователь успешно создан",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(value = USER_JSON))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_400_JSON))),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_401_JSON))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав (требуется роль ADMIN)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_403_JSON))),
            @ApiResponse(responseCode = "409", description = "Пользователь с такой почтой уже существует",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_409_JSON)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest createRequest
    );

    @Operation(
            summary = "Частично обновить информацию о пользователе",
            description = "Обновление профиля, департамента или роли пользователя. Обновляются только переданные " +
                    "поля, остальные значения сохраняются. Доступно только администраторам"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Информация о пользователе после обновления",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(value = USER_JSON))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_400_JSON))),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_401_JSON))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав (требуется роль ADMIN)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_403_JSON))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_404_JSON)))
    })
    @PatchMapping("/{id}")
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
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Пользователь успешно удалён"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_401_JSON))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав (требуется роль ADMIN)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_403_JSON))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_404_JSON)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteUser(
            @Parameter(description = "Уникальный идентификатор пользователя", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "ID не может быть меньше нуля") Long id
    );
}