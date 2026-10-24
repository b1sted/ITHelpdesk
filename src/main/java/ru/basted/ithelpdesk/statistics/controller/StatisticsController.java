package ru.basted.ithelpdesk.statistics.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.basted.ithelpdesk.statistics.dto.AgentStatisticsResponse;
import ru.basted.ithelpdesk.statistics.dto.PeriodStatisticsResponse;
import ru.basted.ithelpdesk.statistics.dto.StatisticsGranularity;
import ru.basted.ithelpdesk.statistics.service.StatisticsService;

@RestController
@RequestMapping("/api/v1")
public class StatisticsController implements StatisticsAPI {
    private final StatisticsService service;

    public StatisticsController(StatisticsService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<List<AgentStatisticsResponse>> getAgentStatistics() {
        return ResponseEntity.ok(service.getAgentStatistics());
    }

    @Override
    public ResponseEntity<PeriodStatisticsResponse> getPeriodStatistics(
            StatisticsGranularity granularity
    ) {
        return ResponseEntity.ok(service.getPeriodStatistics(granularity));
    }
}
