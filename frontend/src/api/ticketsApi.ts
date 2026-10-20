import { client } from './client';
import type {
  MessageResponse,
  TicketCreateRequest,
  TicketResponse,
  TicketStatus,
  TicketStatusChangeRequest,
} from '../types/ticket';

export async function createTicket(data: TicketCreateRequest): Promise<TicketResponse> {
  const response = await client.post<TicketResponse>('/tickets', data);
  return response.data;
}

export async function getTickets(status?: TicketStatus): Promise<TicketResponse[]> {
  const response = await client.get<TicketResponse[]>('/tickets', {
    params: status ? { status } : {},
  });
  return response.data;
}

export async function getTicket(id: number): Promise<TicketResponse> {
  const response = await client.get<TicketResponse>(`/tickets/${id}`);
  return response.data;
}

export async function assignTicket(id: number): Promise<TicketResponse> {
  const response = await client.patch<TicketResponse>(`/tickets/${id}/assign`);
  return response.data;
}

export async function changeTicketStatus(
  id: number,
  data: TicketStatusChangeRequest,
): Promise<TicketResponse> {
  const response = await client.patch<TicketResponse>(`/tickets/${id}/status`, data);
  return response.data;
}

export async function addMessage(id: number, text: string): Promise<MessageResponse> {
  const response = await client.post<MessageResponse>(`/tickets/${id}/messages`, { text });
  return response.data;
}

export async function getCategories(): Promise<{ id: number; name: string }[]> {
  const response = await client.get<{ id: number; name: string }[]>('/categories');
  return response.data;
}