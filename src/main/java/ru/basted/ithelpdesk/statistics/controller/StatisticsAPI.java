package ru.basted.ithelpdesk.statistics.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.basted.ithelpdesk.core.exception.ApiError;
import ru.basted.ithelpdesk.statistics.dto.AgentStatisticsResponse;

@Tag(name = "Статистика", description = "Аналитика по работе специалистов поддержки")
public interface StatisticsAPI {
    String AGENT_STATISTICS_JSON = """
            [
              {
                "agentId": 2,
                "fullName": "Петров Пётр Петрович",
                "email": "petrov.pp@company.ru",
                "departmentName": "IT Service Desk",
                "ticketLimit": 10,
                "totalAssigned": 42,
                "activeTickets": 5,
                "resolvedTickets": 20,
                "closedTickets": 15,
                "overdueTickets": 2
              }
            ]""";

    @Operation(
            summary = "Получить статистику по специалистам поддержки",
            description = "Возвращает количество назначенных, активных, решённых, закрытых и просроченных "
                    + "заявок по каждому специалисту. Доступно руководителю отдела и администратору"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Статистика по специалистам",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = AgentStatisticsResponse.class)),
                            examples = @ExampleObject(value = AGENT_STATISTICS_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Ресурс не найден или недостаточно прав",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = "{\"message\":\"Ресурс не найден\"}")))
    })
    @GetMapping("/statistics/agents")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    ResponseEntity<List<AgentStatisticsResponse>> getAgentStatistics();
}
