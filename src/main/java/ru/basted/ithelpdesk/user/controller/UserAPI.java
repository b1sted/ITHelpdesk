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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.basted.ithelpdesk.core.exception.ApiError;
import ru.basted.ithelpdesk.user.dto.UserCreateRequest;
import ru.basted.ithelpdesk.user.dto.UserResponse;
import ru.basted.ithelpdesk.user.dto.UserLimitsRequest;
import ru.basted.ithelpdesk.user.dto.UserUpdateRequest;

@Tag(name = "Пользователи", description = "Управление пользователями и каталогом сотрудников")
public interface UserAPI {
    String USER_JSON = """
            {
              "id": 1,
              "fullName": "Иванов Иван Иванович",
              "email": "ivanov.ii@company.ru",
              "role": "EMPLOYEE",
              "departmentName": "Департамент тестирования программных продуктов",
              "ticketLimit": 10,
              "timeLimitHours": 24
            }""";
    String USER_LIST_JSON = "[" + USER_JSON + "]";
    String ERROR_404_GENERIC_JSON = "{\"message\":\"Ресурс не найден\"}";
    String ERROR_404_USER_JSON = "{\"message\":\"Не найден пользователь с ID 99\"}";
    String ERROR_400_JSON = "{\"message\":\"Некорректный формат электронной почты\"}";
    String ERROR_409_JSON = "{\"message\":\"Пользователь с электронной почтой ivanov.ii@company.ru уже существует\"}";

    @Operation(
            summary = "Получить список всех пользователей",
            description = "Возвращает список всех зарегистрированных пользователей. Доступно только администраторам"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список пользователей",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = UserResponse.class)),
                            examples = @ExampleObject(value = USER_LIST_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Ресурс не найден или недостаточно прав",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_404_GENERIC_JSON)))
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
            @ApiResponse(responseCode = "404",
                    description = "Пользователь не найден или ресурс скрыт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Не найден пользователь", value = ERROR_404_USER_JSON),
                                    @ExampleObject(name = "Ресурс скрыт", value = ERROR_404_GENERIC_JSON)
                            }))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#id)")
    ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "Уникальный идентификатор пользователя", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "Идентификатор должен быть положительным") Long id
    );

    @Operation(
            summary = "Получить список специалистов поддержки",
            description = "Возвращает список специалистов для выбора при переназначении заявки"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список специалистов поддержки",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = UserResponse.class)),
                            examples = @ExampleObject(value = USER_LIST_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Ресурс не найден или требуется аутентификация",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_404_GENERIC_JSON)))
    })
    @GetMapping("/agents")
    @PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
    ResponseEntity<List<UserResponse>> getAgents();

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
            @ApiResponse(responseCode = "404",
                    description = "Ресурс скрыт или недостаточно прав",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_404_GENERIC_JSON))),
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
            @ApiResponse(responseCode = "404",
                    description = "Пользователь не найден или ресурс скрыт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Не найден пользователь", value = ERROR_404_USER_JSON),
                                    @ExampleObject(name = "Ресурс скрыт", value = ERROR_404_GENERIC_JSON)
                            }))
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "Уникальный идентификатор пользователя", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "Идентификатор должен быть положительным") Long id,

            @Valid @RequestBody UserUpdateRequest updateRequest
    );

    @Operation(
            summary = "Изменить лимиты специалиста поддержки",
            description = "Устанавливает максимальное число одновременно взятых в работу заявок и предельное "
                    + "время выполнения назначенной заявки. Доступно руководителю и администратору"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Лимиты обновлены",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(value = USER_JSON))),
            @ApiResponse(responseCode = "400", description = "Некорректные лимиты",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_400_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Пользователь не найден или ресурс скрыт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Не найден пользователь", value = ERROR_404_USER_JSON),
                                    @ExampleObject(name = "Ресурс скрыт", value = ERROR_404_GENERIC_JSON)
                            })),
            @ApiResponse(responseCode = "409", description = "Лимиты применимы только к специалистам",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Лимиты применяются только к специалистам поддержки\"}")))
    })
    @PatchMapping("/{id}/limits")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    ResponseEntity<UserResponse> updateLimits(
            @Parameter(description = "Уникальный идентификатор пользователя", required = true, example = "2")
            @PathVariable("id")
            @Min(value = 1, message = "Идентификатор должен быть положительным") Long id,

            @Valid @RequestBody UserLimitsRequest request
    );

    @Operation(
            summary = "Удалить пользователя",
            description = "Удаление или деактивация пользователя. Доступно только администраторам"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Пользователь успешно удалён"),
            @ApiResponse(responseCode = "404",
                    description = "Пользователь не найден или ресурс скрыт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Не найден пользователь", value = ERROR_404_USER_JSON),
                                    @ExampleObject(name = "Ресурс скрыт", value = ERROR_404_GENERIC_JSON)
                            }))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteUser(
            @Parameter(description = "Уникальный идентификатор пользователя", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "Идентификатор должен быть положительным") Long id
    );
}