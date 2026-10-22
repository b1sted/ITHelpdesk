import { client } from './client';
import type { AgentStatisticsResponse } from '../types/ticket';

export async function getAgentStatistics(): Promise<AgentStatisticsResponse[]> {
  const response = await client.get<AgentStatisticsResponse[]>('/statistics/agents');
  return response.data;
}
