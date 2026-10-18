import { client } from './client';
import type {
  CommentResponse,
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

export async function addComment(id: number, text: string): Promise<CommentResponse> {
  const response = await client.post<CommentResponse>(`/tickets/${id}/comments`, { text });
  return response.data;
}

export async function getCategories(): Promise<{ id: number; name: string }[]> {
  const response = await client.get<{ id: number; name: string }[]>('/categories');
  return response.data;
}