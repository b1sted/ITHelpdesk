import { useState } from 'react';
import type {
  TicketRequestResponse,
  TicketRequestStatus,
} from '../types/ticket';

interface TicketRequestListProps {
  requests: TicketRequestResponse[];
  showTicket?: boolean;
  onDecide?: (
    requestId: number,
    status: Exclude<TicketRequestStatus, 'PENDING'>,
    comment?: string,
  ) => Promise<void>;
}

function formatDate(value: string): string {
  return new Date(value).toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

const typeLabels = {
  RELEASE: 'Отказ от заявки',
  REASSIGN: 'Переназначение',
};

const statusLabels: Record<TicketRequestStatus, { label: string; classes: string }> = {
  PENDING: { label: 'Ожидает решения', classes: 'bg-amber-100 text-amber-700' },
  APPROVED: { label: 'Одобрен', classes: 'bg-emerald-100 text-emerald-700' },
  REJECTED: { label: 'Отклонён', classes: 'bg-red-100 text-red-700' },
};

export function TicketRequestList({
  requests,
  showTicket = false,
  onDecide,
}: TicketRequestListProps) {
  if (requests.length === 0) {
    return <p className="text-sm text-slate-400">Запросов пока нет.</p>;
  }

  return (
    <div className="space-y-3">
      {requests.map((request) => (
        <RequestRow
          key={request.id}
          request={request}
          showTicket={showTicket}
          onDecide={onDecide}
        />
      ))}
    </div>
  );
}

function RequestRow({
  request,
  showTicket,
  onDecide,
}: {
  request: TicketRequestResponse;
  showTicket: boolean;
  onDecide?: TicketRequestListProps['onDecide'];
}) {
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const status = statusLabels[request.status];

  const decide = async (next: 'APPROVED' | 'REJECTED') => {
    if (!onDecide || submitting) {
      return;
    }
    setSubmitting(true);
    try {
      await onDecide(request.id, next, comment.trim() || undefined);
      setComment('');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-3">
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-sm font-medium text-slate-800">
          {typeLabels[request.type]}
        </span>
        {showTicket && (
          <span className="text-xs font-medium text-emerald-700">#{request.ticketId}</span>
        )}
        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${status.classes}`}>
          {status.label}
        </span>
        <span className="ml-auto text-xs text-slate-400">{formatDate(request.createdAt)}</span>
      </div>
      <p className="mt-2 text-sm text-slate-600">{request.reason}</p>
      <p className="mt-1 text-xs text-slate-400">
        Инициатор: {request.requester.fullName}
        {request.targetAssignee ? ` · Новый специалист: ${request.targetAssignee.fullName}` : ''}
      </p>
      {request.decidedBy && request.decidedAt && (
        <p className="mt-1 text-xs text-slate-400">
          Решение: {request.decidedBy.fullName}, {formatDate(request.decidedAt)}
          {request.decisionComment ? ` · ${request.decisionComment}` : ''}
        </p>
      )}
      {request.status === 'PENDING' && onDecide && (
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <input
            value={comment}
            onChange={(event) => setComment(event.target.value)}
            placeholder="Комментарий руководителя (необязательно)"
            className="min-w-[220px] flex-1 rounded-md border border-slate-300 px-3 py-1.5 text-sm focus:border-emerald-500 focus:outline-none"
          />
          <button
            onClick={() => decide('APPROVED')}
            disabled={submitting}
            className="rounded-md bg-emerald-600 px-3 py-1.5 text-sm font-medium text-white transition hover:bg-emerald-700 disabled:opacity-50"
          >
            Одобрить
          </button>
          <button
            onClick={() => decide('REJECTED')}
            disabled={submitting}
            className="rounded-md border border-red-300 px-3 py-1.5 text-sm font-medium text-red-600 transition hover:bg-red-50 disabled:opacity-50"
          >
            Отклонить
          </button>
        </div>
      )}
    </div>
  );
}
