import { client } from './client';
import type {
  AgentStatisticsResponse,
  PeriodStatisticsResponse,
  StatisticsGranularity,
  SupportSettingsResponse,
} from '../types/ticket';

export async function getAgentStatistics(): Promise<AgentStatisticsResponse[]> {
  const response = await client.get<AgentStatisticsResponse[]>('/statistics/agents');
  return response.data;
}

export async function getSupportSettings(): Promise<SupportSettingsResponse> {
  const response = await client.get<SupportSettingsResponse>('/settings/support');
  return response.data;
}

export async function updateSupportSettings(
  autoCloseResolvedHours: number,
): Promise<SupportSettingsResponse> {
  const response = await client.put<SupportSettingsResponse>('/settings/support', {
    autoCloseResolvedHours,
  });
  return response.data;
}

export async function getPeriodStatistics(
  granularity: StatisticsGranularity,
): Promise<PeriodStatisticsResponse> {
  const response = await client.get<PeriodStatisticsResponse>('/statistics/period', {
    params: { granularity },
  });
  return response.data;
}
