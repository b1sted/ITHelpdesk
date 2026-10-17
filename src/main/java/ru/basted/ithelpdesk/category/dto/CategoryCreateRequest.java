package ru.basted.ithelpdesk.category.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель данных для добавления новой категории в базу данных")
public record CategoryCreateRequest(
    @NotBlank(message = "Имя категории не может быть пустым")
    @Schema(description = "Наименование категории", example = "Оборудование")
    String name
) {}