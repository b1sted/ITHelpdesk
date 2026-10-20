package ru.basted.ithelpdesk.department.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.MediaType;
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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.basted.ithelpdesk.core.exception.ApiError;
import ru.basted.ithelpdesk.department.dto.DepartmentChangeName;
import ru.basted.ithelpdesk.department.dto.DepartmentCreateRequest;
import ru.basted.ithelpdesk.department.dto.DepartmentResponse;

@Tag(name = "Департаменты", description = "Управление базой данных департаментами")
public interface DepartmentAPI {
    String DEPARTMENT_JSON = """
            {
              "id": 1,
              "name": "Департамент тестирования программных продуктов"
            }""";
    String DEPARTMENT_LIST_JSON = "[" + DEPARTMENT_JSON + "]";
    String ERROR_404_GENERIC_JSON = "{\"message\":\"Ресурс не найден\"}";
    String ERROR_404_DEPARTMENT_JSON = "{\"message\":\"Не найден департамент с ID 99\"}";
    String ERROR_400_JSON = "{\"message\":\"Имя департамента не может быть пустым\"}";
    String ERROR_409_JSON = "{\"message\":\"Департамент с именем Дубликат уже существует в базе данных\"}";

    @Operation(
            summary = "Получить список департаментов, зарегистрированных в базе данных",
            description = "Возвращает список всех зарегистрированных департаментов с информацией о них."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список департаментов",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = DepartmentResponse.class)),
                            examples = @ExampleObject(value = DEPARTMENT_LIST_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Ресурс не найден или требуется аутентификация",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_404_GENERIC_JSON)))
    })
    @GetMapping
    ResponseEntity<List<DepartmentResponse>> getAllDepartments();

    @Operation(
            summary = "Получить информацию о департаменте по его уникальному идентификатору",
            description = "Возвращает информацию о конкретном департаменте, если он зарегистрирован в базе данных, " +
                    "по его уникальному идентификатору. Доступна всем авторизованным пользователям"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Информация о департаменте",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DepartmentResponse.class),
                            examples = @ExampleObject(value = DEPARTMENT_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Департамент не найден или ресурс скрыт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Не найден департамент", value = ERROR_404_DEPARTMENT_JSON),
                                    @ExampleObject(name = "Ресурс скрыт", value = ERROR_404_GENERIC_JSON)
                            }))
    })
    @GetMapping("/{id}")
    ResponseEntity<DepartmentResponse> getDepartmentById(
            @Parameter(description = "Уникальный идентификатор департамента", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "Идентификатор должен быть положительным") Long id
    );

    @Operation(
            summary = "Создать новый департамент",
            description = "Создание записи в базе данных о новом департаменте. Доступно только администраторам"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Департамент успешно создан",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DepartmentResponse.class),
                            examples = @ExampleObject(value = DEPARTMENT_JSON))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_400_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Ресурс скрыт или недостаточно прав",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_404_GENERIC_JSON))),
            @ApiResponse(responseCode = "409", description = "Департамент с таким именем уже существует",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_409_JSON)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody DepartmentCreateRequest createRequest
    );

    @Operation(
            summary = "Смена имени департамента",
            description = "Изменение имени существующего департамента. Доступна только администраторам."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Имя департамента успешно изменено",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DepartmentResponse.class),
                            examples = @ExampleObject(value = DEPARTMENT_JSON))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_400_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Департамент не найден или ресурс скрыт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Не найден департамент", value = ERROR_404_DEPARTMENT_JSON),
                                    @ExampleObject(name = "Ресурс скрыт", value = ERROR_404_GENERIC_JSON)
                            }))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<DepartmentResponse> updateName(
            @Parameter(description = "Уникальный идентификатор департамента", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "Идентификатор должен быть положительным") Long id,

            @Valid @RequestBody DepartmentChangeName changeName
    );

    @Operation(
            summary = "Удаление департамента",
            description = "Удаление департамента. Доступна только администраторам."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Департамент успешно удалён"),
            @ApiResponse(responseCode = "404",
                    description = "Департамент не найден или ресурс скрыт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Не найден департамент", value = ERROR_404_DEPARTMENT_JSON),
                                    @ExampleObject(name = "Ресурс скрыт", value = ERROR_404_GENERIC_JSON)
                            }))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteDepartment(
            @Parameter(description = "Уникальный идентификатор департамента", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "Идентификатор должен быть положительным") Long id
    );
}