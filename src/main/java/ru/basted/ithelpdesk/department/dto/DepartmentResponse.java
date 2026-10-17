package ru.basted.ithelpdesk.department.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель данных, возвращаемая при создании или получении информации о департаменте")
public record DepartmentResponse(
    @Schema(description = "Уникальный идентификатор департамента", example = "1")
    Long id,

    @Schema(description = "Наименование департамента", example = "Департамент тестирования программных продуктов")
    String name
) {}