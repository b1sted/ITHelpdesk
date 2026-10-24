package ru.basted.ithelpdesk.statistics.service;

import java.util.List;

import ru.basted.ithelpdesk.statistics.dto.AgentStatisticsResponse;
import ru.basted.ithelpdesk.statistics.dto.PeriodStatisticsResponse;
import ru.basted.ithelpdesk.statistics.dto.StatisticsGranularity;

public interface StatisticsService {
    List<AgentStatisticsResponse> getAgentStatistics();

    PeriodStatisticsResponse getPeriodStatistics(StatisticsGranularity granularity);
}
