package ru.basted.ithelpdesk.departments.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель данных, возвращаемая при создании или получении информации о департаменте")
public record DepartmentsResponse(
        @Schema(description = "Уникальный идентификатор департамента", example = "1")
        Long id,

        @Schema(description = "Наименование департамента", example = "Департамент тестирования программных продуктов")
        String name
) { }
