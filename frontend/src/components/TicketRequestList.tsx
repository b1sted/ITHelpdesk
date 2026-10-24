import { useState } from 'react';
import type {
  TicketRequestResponse,
  TicketRequestStatus,
} from '../types/ticket';
import type { UserResponse } from '../types/user';
import { formatDateTime } from '../utils/formatDate';

interface TicketRequestListProps {
  requests: TicketRequestResponse[];
  agents?: UserResponse[];
  showTicket?: boolean;
  onDecide?: (
    requestId: number,
    status: Exclude<TicketRequestStatus, 'PENDING'>,
    comment?: string,
    targetAssigneeId?: number,
  ) => Promise<void>;
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
  agents = [],
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
          agents={agents}
          showTicket={showTicket}
          onDecide={onDecide}
        />
      ))}
    </div>
  );
}

function RequestRow({
  request,
  agents,
  showTicket,
  onDecide,
}: {
  request: TicketRequestResponse;
  agents: UserResponse[];
  showTicket: boolean;
  onDecide?: TicketRequestListProps['onDecide'];
}) {
  const [comment, setComment] = useState('');
  const [targetAssigneeId, setTargetAssigneeId] = useState(
    request.targetAssignee ? String(request.targetAssignee.id) : '',
  );
  const [submitting, setSubmitting] = useState(false);
  const status = statusLabels[request.status];
  const canChooseTarget = request.type === 'REASSIGN' && request.status === 'PENDING';
  const targetRequired = canChooseTarget && !targetAssigneeId;

  const decide = async (next: 'APPROVED' | 'REJECTED') => {
    if (!onDecide || submitting) {
      return;
    }
    setSubmitting(true);
    try {
      await onDecide(
        request.id,
        next,
        comment.trim() || undefined,
        targetAssigneeId ? Number(targetAssigneeId) : undefined,
      );
      setComment('');
      setTargetAssigneeId('');
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
        <span className="ml-auto text-xs text-slate-400">{formatDateTime(request.createdAt)}</span>
      </div>
      <p className="mt-2 text-sm text-slate-600">{request.reason}</p>
      <p className="mt-1 text-xs text-slate-400">
        Инициатор: {request.requester.fullName}
        {request.targetAssignee
          ? ` · Новый специалист: ${request.targetAssignee.fullName}`
          : request.type === 'REASSIGN' && request.status === 'PENDING'
            ? ' · Специалиста выбирает руководитель'
            : ''}
      </p>
      {request.decidedBy && request.decidedAt && (
        <p className="mt-1 text-xs text-slate-400">
          Решение: {request.decidedBy.fullName}, {formatDateTime(request.decidedAt)}
          {request.decisionComment ? ` · ${request.decisionComment}` : ''}
        </p>
      )}
      {request.status === 'PENDING' && onDecide && (
        <div className="mt-3 space-y-2">
          {canChooseTarget && (
            <select
              value={targetAssigneeId}
              onChange={(event) => setTargetAssigneeId(event.target.value)}
              className="w-full rounded-md border border-slate-300 px-3 py-1.5 text-sm focus:border-emerald-500 focus:outline-none"
            >
              <option value="">Выберите специалиста для переназначения</option>
              {agents
                .filter((agent) => agent.id !== request.requester.id)
                .map((agent) => (
                  <option key={agent.id} value={agent.id}>
                    {agent.fullName}
                  </option>
                ))}
            </select>
          )}
          <div className="flex flex-wrap items-center gap-2">
          <input
            value={comment}
            onChange={(event) => setComment(event.target.value)}
            placeholder="Комментарий руководителя (необязательно)"
            className="min-w-[220px] flex-1 rounded-md border border-slate-300 px-3 py-1.5 text-sm focus:border-emerald-500 focus:outline-none"
          />
          <button
            onClick={() => decide('APPROVED')}
            disabled={submitting || targetRequired}
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
        </div>
      )}
    </div>
  );
}
