package ru.basted.ithelpdesk.statistics.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
import ru.basted.ithelpdesk.statistics.dto.AgentStatisticsResponse;
import ru.basted.ithelpdesk.statistics.dto.PeriodStatisticsResponse;
import ru.basted.ithelpdesk.statistics.dto.StatisticsGranularity;

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
                "timeLimitHours": 24,
                "totalAssigned": 42,
                "activeTickets": 5,
                "resolvedTickets": 20,
                "closedTickets": 15,
                "overdueTickets": 2
              }
            ]""";
    String PERIOD_STATISTICS_JSON = """
            {
              "granularity": "MONTH",
              "buckets": [
                {"label": "янв.", "created": 12, "resolved": 10, "closed": 8},
                {"label": "фев.", "created": 15, "resolved": 13, "closed": 11}
              ],
              "totalCreated": 27,
              "totalResolved": 23,
              "totalClosed": 19,
              "averageResolutionHours": 18.5
            }""";

    @Operation(
            summary = "Получить статистику по специалистам поддержки",
            description = "Возвращает количество назначенных, активных, решённых, закрытых и просроченных "
                    + "заявок по каждому специалисту. Доступно руководителю и администратору"
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

    @Operation(
            summary = "Получить статистику поддержки за месяцы или годы",
            description = "Возвращает количество созданных, решённых и закрытых заявок по месяцам "
                    + "(с месяца первой заявки по текущий месяц) или по годам "
                    + "(с года первой заявки; не менее пяти лет с пустыми будущими годами), "
                    + "а также среднее время решения. Доступно руководителю и администратору"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Статистика по периодам",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PeriodStatisticsResponse.class),
                            examples = @ExampleObject(value = PERIOD_STATISTICS_JSON))),
            @ApiResponse(responseCode = "404",
                    description = "Ресурс не найден или недостаточно прав",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = "{\"message\":\"Ресурс не найден\"}")))
    })
    @GetMapping("/statistics/period")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    ResponseEntity<PeriodStatisticsResponse> getPeriodStatistics(
            @Parameter(description = "Гранулярность: MONTH — по месяцам, YEAR — по годам")
            @RequestParam(required = false, defaultValue = "MONTH")
            StatisticsGranularity granularity
    );
}
