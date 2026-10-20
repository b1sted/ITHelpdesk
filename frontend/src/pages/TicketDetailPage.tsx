import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import {
  addComment,
  assignTicket,
  changeTicketStatus,
  getTicket,
} from '../api/ticketsApi';
import { CommentThread } from '../components/CommentThread';
import { StatusBadge } from '../components/StatusBadge';
import { useAuth } from '../context/AuthContext';
import type { TicketStatus, TicketResponse } from '../types/ticket';

function formatDate(value: string): string {
  return new Date(value).toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function TicketDetailPage() {
  const { id } = useParams<{ id: string }>();
  const ticketId = Number(id);
  const { user } = useAuth();
  const navigate = useNavigate();
  const [ticket, setTicket] = useState<TicketResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reopenComment, setReopenComment] = useState('');

  const loadTicket = useCallback(() => {
    setLoading(true);
    getTicket(ticketId)
      .then(setTicket)
      .catch(() => setError('Не удалось загрузить заявку'))
      .finally(() => setLoading(false));
  }, [ticketId]);

  useEffect(() => {
    loadTicket();
  }, [loadTicket]);

  if (loading) {
    return <p className="py-10 text-center text-sm text-slate-400">Загрузка...</p>;
  }

  if (error || !ticket) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-10 text-center">
        <p className="text-sm text-red-600">{error || 'Заявка не найдена'}</p>
        <button
          onClick={() => navigate('/')}
          className="mt-4 text-sm text-emerald-600 hover:underline"
        >
          Вернуться к списку
        </button>
      </div>
    );
  }

const isAgent = user?.role === 'AGENT';
  const isAuthor = user?.email === ticket.creator.email;
  const isAssignee = user?.email === ticket.assignee?.email;

  const refresh = async () => {
    const updated = await getTicket(ticket.id);
    setTicket(updated);
  };

  const handleAssign = async () => {
    await assignTicket(ticket.id);
    await refresh();
  };

  const handleStatus = async (status: 'WAITING_USER' | 'RESOLVED' | 'CLOSED' | 'IN_PROGRESS') => {
    await changeTicketStatus(ticket.id, {
      status,
      comment: status === 'IN_PROGRESS' ? reopenComment : undefined,
    });
    setReopenComment('');
    await refresh();
  };

  const handleAddComment = async (text: string) => {
    await addComment(ticket.id, text);
    await refresh();
  };

  const canAgentWork =
    isAgent &&
    (ticket.status === 'NEW' ||
      (isAssignee && ['IN_PROGRESS', 'WAITING_USER', 'ANSWERED'].includes(ticket.status)));
  const canAuthorWork = isAuthor && ticket.status === 'RESOLVED';
  const canComment = !['CLOSED'].includes(ticket.status);

  return (
    <div className="mx-auto max-w-4xl px-4 py-6">
      <button
        onClick={() => navigate('/')}
        className="mb-4 flex items-center gap-1 text-sm text-slate-500 transition hover:text-slate-700"
      >
        <ArrowLeft className="h-4 w-4" />
        К списку заявок
      </button>

      <div className="rounded-lg bg-white p-6 shadow">
        <div className="mb-4 flex flex-wrap items-center gap-3">
          <span className="text-lg font-semibold text-slate-800">#{ticket.id}</span>
          <StatusBadge status={ticket.status} />
          <span className="text-sm text-slate-400">{ticket.categoryName}</span>
        </div>
        <h1 className="mb-2 text-xl font-semibold text-slate-900">{ticket.title}</h1>
        <p className="mb-6 whitespace-pre-wrap text-sm text-slate-600">{ticket.description}</p>

        <div className="grid grid-cols-1 gap-4 border-t border-slate-100 pt-4 text-sm sm:grid-cols-3">
          <div>
            <div className="text-xs uppercase text-slate-400">Автор</div>
            <div className="font-medium text-slate-700">{ticket.creator.fullName}</div>
            <div className="text-xs text-slate-400">{ticket.creator.departmentName}</div>
          </div>
          <div>
            <div className="text-xs uppercase text-slate-400">Исполнитель</div>
            {ticket.assignee ? (
              <>
                <div className="font-medium text-slate-700">{ticket.assignee.fullName}</div>
                <div className="text-xs text-slate-400">{ticket.assignee.departmentName}</div>
              </>
            ) : (
              <div className="text-slate-400">Не назначен</div>
            )}
          </div>
          <div>
            <div className="text-xs uppercase text-slate-400">Создана</div>
            <div className="font-medium text-slate-700">{formatDate(ticket.createdAt)}</div>
          </div>
        </div>

        {canAgentWork && <AgentPanel onAssign={handleAssign} onStatus={handleStatus} status={ticket.status} />}

        {canAuthorWork && (
          <div className="mt-6 border-t border-slate-100 pt-4">
            <div className="flex flex-wrap items-center gap-2">
              <button
                onClick={() => handleStatus('CLOSED')}
                className="rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-700"
              >
                Закрыть заявку
              </button>
              <button
                onClick={() => handleStatus('IN_PROGRESS')}
                disabled={!reopenComment.trim()}
                className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Вернуть на доработку
              </button>
            </div>
            <input
              type="text"
              value={reopenComment}
              onChange={(event) => setReopenComment(event.target.value)}
              placeholder="Комментарий для возврата на доработку (обязательно)"
              className="mt-3 w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
            />
          </div>
        )}

        <CommentThread
          comments={ticket.comments}
          onAddComment={handleAddComment}
          canComment={canComment}
        />
      </div>
    </div>
  );
}

interface AgentPanelProps {
  status?: TicketStatus;
  onAssign?: () => Promise<void>;
  onStatus?: (status: 'WAITING_USER' | 'RESOLVED' | 'IN_PROGRESS') => Promise<void>;
}

function AgentPanel({ status, onAssign, onStatus }: AgentPanelProps) {
  if (status === 'NEW') {
    return (
      <div className="mt-6 flex flex-wrap gap-2 border-t border-slate-100 pt-4">
        <button
          onClick={onAssign}
          className="rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-700"
        >
          Взять в работу
        </button>
      </div>
    );
  }

  return (
    <div className="mt-6 flex flex-wrap gap-2 border-t border-slate-100 pt-4">
      {status === 'IN_PROGRESS' && (
        <button
          onClick={() => onStatus?.('WAITING_USER')}
          className="rounded-md bg-amber-500 px-4 py-2 text-sm font-medium text-white transition hover:bg-amber-600"
        >
          Жду ответа
        </button>
      )}
      {status === 'ANSWERED' && (
        <button
          onClick={() => onStatus?.('IN_PROGRESS')}
          className="rounded-md bg-slate-700 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800"
        >
          Продолжить работу
        </button>
      )}
      <button
        onClick={() => onStatus?.('RESOLVED')}
        className="rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-700"
      >
        Решено
      </button>
    </div>
  );
}