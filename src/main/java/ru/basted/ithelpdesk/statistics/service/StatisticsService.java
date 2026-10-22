package ru.basted.ithelpdesk.statistics.service;

import java.util.List;

import ru.basted.ithelpdesk.statistics.dto.AgentStatisticsResponse;

public interface StatisticsService {
    List<AgentStatisticsResponse> getAgentStatistics();
}
