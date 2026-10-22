import { client } from './client';
import type {
  TicketRequestCreateRequest,
  TicketRequestDecisionRequest,
  TicketRequestResponse,
  TicketRequestStatus,
} from '../types/ticket';

export async function createTicketRequest(
  ticketId: number,
  data: TicketRequestCreateRequest,
): Promise<TicketRequestResponse> {
  const response = await client.post<TicketRequestResponse>(`/tickets/${ticketId}/requests`, data);
  return response.data;
}

export async function getTicketRequests(ticketId: number): Promise<TicketRequestResponse[]> {
  const response = await client.get<TicketRequestResponse[]>(`/tickets/${ticketId}/requests`);
  return response.data;
}

export async function getTicketRequestsByStatus(
  status?: TicketRequestStatus,
): Promise<TicketRequestResponse[]> {
  const response = await client.get<TicketRequestResponse[]>('/requests', {
    params: status ? { status } : {},
  });
  return response.data;
}

export async function decideTicketRequest(
  requestId: number,
  data: TicketRequestDecisionRequest,
): Promise<TicketRequestResponse> {
  const response = await client.patch<TicketRequestResponse>(
    `/requests/${requestId}/decision`,
    data,
  );
  return response.data;
}
