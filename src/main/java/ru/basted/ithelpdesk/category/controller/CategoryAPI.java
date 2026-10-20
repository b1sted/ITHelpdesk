package ru.basted.ithelpdesk.category.controller;

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

import ru.basted.ithelpdesk.category.dto.CategoryChangeName;
import ru.basted.ithelpdesk.category.dto.CategoryCreateRequest;
import ru.basted.ithelpdesk.category.dto.CategoryResponse;
import ru.basted.ithelpdesk.core.exception.ApiError;

@Tag(name = "Категории", description = "Управление категориями инцидентов")
public interface CategoryAPI {
    String CATEGORY_JSON = """
            {
              "id": 1,
              "name": "Оборудование"
            }""";
    String CATEGORY_LIST_JSON = "[" + CATEGORY_JSON + "]";
    String ERROR_404_GENERIC_JSON = "{\"message\":\"Ресурс не найден\"}";
    String ERROR_404_CATEGORY_JSON = "{\"message\":\"Не найдена категория с ID 99\"}";
    String ERROR_400_JSON = "{\"message\":\"Имя категории не может быть пустым\"}";
    String ERROR_409_JSON = "{\"message\":\"Категория с именем Дубликат уже существует в базе данных\"}";

    @Operation(
            summary = "Получить список всех категорий",
            description = "Возвращает список всех зарегистрированных категорий инцидентов"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список категорий",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CategoryResponse.class)),
                            examples = @ExampleObject(value = CATEGORY_LIST_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Ресурс не найден или требуется аутентификация",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_404_GENERIC_JSON)))
    })
    @GetMapping
    ResponseEntity<List<CategoryResponse>> getAllCategories();

    @Operation(
            summary = "Получить информацию о категории по её уникальному идентификатору",
            description = "Возвращает информацию о конкретной категории, если она зарегистрирована в базе данных, " +
                    "по её уникальному идентификатору"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Информация о категории",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CategoryResponse.class),
                            examples = @ExampleObject(value = CATEGORY_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Категория не найдена или ресурс скрыт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Не найдена категория", value = ERROR_404_CATEGORY_JSON),
                                    @ExampleObject(name = "Ресурс скрыт", value = ERROR_404_GENERIC_JSON)
                            }))
    })
    @GetMapping("/{id}")
    ResponseEntity<CategoryResponse> getCategoryById(
            @Parameter(description = "Уникальный идентификатор категории", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "Идентификатор должен быть положительным") Long id
    );

    @Operation(
            summary = "Создать новую категорию",
            description = "Создание записи в базе данных о новой категории инцидентов. Доступно только администраторам"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Категория успешно создана",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CategoryResponse.class),
                            examples = @ExampleObject(value = CATEGORY_JSON))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_400_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Ресурс скрыт или недостаточно прав",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_404_GENERIC_JSON))),
            @ApiResponse(responseCode = "409", description = "Категория с таким именем уже существует",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_409_JSON)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest createRequest
    );

    @Operation(
            summary = "Смена имени категории",
            description = "Изменение имени существующей категории. Доступна только администраторам."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Имя категории успешно изменено",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CategoryResponse.class),
                            examples = @ExampleObject(value = CATEGORY_JSON))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ERROR_400_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Категория не найдена или ресурс скрыт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Не найдена категория", value = ERROR_404_CATEGORY_JSON),
                                    @ExampleObject(name = "Ресурс скрыт", value = ERROR_404_GENERIC_JSON)
                            }))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<CategoryResponse> updateName(
            @Parameter(description = "Уникальный идентификатор категории", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "Идентификатор должен быть положительным") Long id,

            @Valid @RequestBody CategoryChangeName changeName
    );

    @Operation(
            summary = "Удаление категории",
            description = "Удаление категории. Доступна только администраторам."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Категория успешно удалена"),
            @ApiResponse(responseCode = "404",
                    description = "Категория не найдена или ресурс скрыт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Не найдена категория", value = ERROR_404_CATEGORY_JSON),
                                    @ExampleObject(name = "Ресурс скрыт", value = ERROR_404_GENERIC_JSON)
                            }))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteCategory(
            @Parameter(description = "Уникальный идентификатор категории", required = true, example = "1")
            @PathVariable("id")
            @Min(value = 1, message = "Идентификатор должен быть положительным") Long id
    );
}