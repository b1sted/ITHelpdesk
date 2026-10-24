import { client } from './client';
import type { AgentStatisticsResponse, SupportSettingsResponse } from '../types/ticket';

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
