package ru.basted.ithelpdesk.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель данных, возвращаемая при создании или получении информации о категории")
public record CategoryResponse(
    @Schema(description = "Уникальный идентификатор категории", example = "1")
    Long id,

    @Schema(description = "Наименование категории", example = "Оборудование")
    String name
) {}