package ru.basted.ithelpdesk.settings.controller;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.basted.ithelpdesk.core.exception.ApiError;
import ru.basted.ithelpdesk.settings.dto.SupportSettingsResponse;
import ru.basted.ithelpdesk.settings.dto.SupportSettingsUpdateRequest;

@Tag(name = "Настройки поддержки", description = "Настройки автоматизации службы поддержки")
public interface SettingsAPI {
    String SETTINGS_JSON = """
            {
              "autoCloseResolvedHours": 72
            }""";

    @Operation(
            summary = "Получить настройки поддержки",
            description = "Возвращает срок автоматического закрытия решённых заявок. "
                    + "Доступно руководителю и администратору"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Настройки поддержки",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SupportSettingsResponse.class),
                            examples = @ExampleObject(value = SETTINGS_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Ресурс не найден или недостаточно прав",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = "{\"message\":\"Ресурс не найден\"}")))
    })
    @GetMapping("/settings/support")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    ResponseEntity<SupportSettingsResponse> getSupportSettings();

    @Operation(
            summary = "Изменить настройки поддержки",
            description = "Задаёт срок автоматического закрытия решённых заявок. 0 отключает автозакрытие. "
                    + "Доступно руководителю и администратору"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Настройки обновлены",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SupportSettingsResponse.class),
                            examples = @ExampleObject(value = SETTINGS_JSON))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Срок автозакрытия не может быть отрицательным\"}"))),
            @ApiResponse(responseCode = "404",
                    description = "Ресурс скрыт или недостаточно прав",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = "{\"message\":\"Ресурс не найден\"}")))
    })
    @PutMapping("/settings/support")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    ResponseEntity<SupportSettingsResponse> updateSupportSettings(
            @Valid @RequestBody SupportSettingsUpdateRequest request
    );
}
