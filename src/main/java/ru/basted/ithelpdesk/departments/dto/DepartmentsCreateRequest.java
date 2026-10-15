package ru.basted.ithelpdesk.departments.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель данных для добавления нового департамента в базу данных")
public record DepartmentsCreateRequest(
    @NotBlank(message = "Имя департамента не может быть пустым")
    @Schema(description = "Наименование департамента", example = "Департамент тестирования программных продуктов")
    String name
) {}
