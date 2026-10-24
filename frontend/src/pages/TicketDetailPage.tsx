import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, TriangleAlert, X } from 'lucide-react';
import { getErrorMessage } from '../api/errorMessage';
import {
  addMessage,
  assignTicket,
  changeTicketStatus,
  getTicket,
} from '../api/ticketsApi';
import { getAgents } from '../api/adminApi';
import {
  createTicketRequest,
  decideTicketRequest,
  getTicketRequests,
} from '../api/ticketRequestsApi';
import { MessageThread } from '../components/MessageThread';
import { StatusBadge } from '../components/StatusBadge';
import { TicketRequestList } from '../components/TicketRequestList';
import { useAuth } from '../context/AuthContext';
import type {
  TicketRequestResponse,
  TicketRequestStatus,
  TicketRequestType,
  TicketStatus,
  TicketResponse,
} from '../types/ticket';
import type { UserResponse } from '../types/user';

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
  const [requests, setRequests] = useState<TicketRequestResponse[]>([]);
  const [agents, setAgents] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [reopenReason, setReopenReason] = useState('');
  const [requestType, setRequestType] = useState<TicketRequestType>('RELEASE');
  const [requestReason, setRequestReason] = useState('');
  const [requestTarget, setRequestTarget] = useState('');
  const [requestOpen, setRequestOpen] = useState(false);
  const [requestSubmitting, setRequestSubmitting] = useState(false);

  const isAgent = user?.role === 'AGENT';
  const isManager = user?.role === 'MANAGER';
  const isAdmin = user?.role === 'ADMIN';
  const canViewRequests = isAgent || isManager || isAdmin;

  const refresh = useCallback(async () => {
    const [updated, updatedRequests] = await Promise.all([
      getTicket(ticketId),
      canViewRequests ? getTicketRequests(ticketId) : Promise.resolve([]),
    ]);
    setTicket(updated);
    setRequests(updatedRequests);
  }, [ticketId, canViewRequests]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError('');

    Promise.all([
      getTicket(ticketId),
      canViewRequests ? getTicketRequests(ticketId) : Promise.resolve([]),
      canViewRequests ? getAgents() : Promise.resolve([]),
    ])
      .then(([data, ticketRequests, agentList]) => {
        if (!cancelled) {
          setTicket(data);
          setRequests(ticketRequests);
          setAgents(agentList);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setError('Не удалось загрузить заявку');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [ticketId, canViewRequests]);

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

  const runAction = async (action: () => Promise<unknown>) => {
    setActionError('');
    try {
      await action();
      await refresh();
    } catch (err: unknown) {
      setActionError(getErrorMessage(err, 'Не удалось выполнить действие'));
    }
  };

  const handleAssign = () => runAction(() => assignTicket(ticket.id));

  const handleStatus = (status: 'WAITING_USER' | 'RESOLVED' | 'CLOSED' | 'IN_PROGRESS') =>
    runAction(async () => {
      await changeTicketStatus(ticket.id, {
        status,
        reason: status === 'IN_PROGRESS' ? reopenReason : undefined,
      });
      setReopenReason('');
    });

  const handleAddMessage = async (text: string) => {
    setActionError('');
    try {
      await addMessage(ticket.id, text);
      await refresh();
    } catch (err: unknown) {
      setActionError(getErrorMessage(err, 'Не удалось отправить сообщение'));
      throw err;
    }
  };

  const handleCreateRequest = async () => {
    setActionError('');
    if (!requestReason.trim()) {
      setActionError('Укажите обоснование запроса');
      return;
    }
    setRequestSubmitting(true);
    try {
      await createTicketRequest(ticket.id, {
        type: requestType,
        reason: requestReason.trim(),
        targetAssigneeId: requestType === 'REASSIGN' ? Number(requestTarget) : undefined,
      });
      setRequestReason('');
      setRequestTarget('');
      setRequestOpen(false);
      await refresh();
    } catch (err: unknown) {
      setActionError(getErrorMessage(err, 'Не удалось создать запрос'));
    } finally {
      setRequestSubmitting(false);
    }
  };

  const handleDecide = async (
    requestId: number,
    status: Exclude<TicketRequestStatus, 'PENDING'>,
    comment?: string,
    targetAssigneeId?: number,
  ) => {
    setActionError('');
    try {
      await decideTicketRequest(requestId, { status, comment, targetAssigneeId });
      await refresh();
    } catch (err: unknown) {
      setActionError(getErrorMessage(err, 'Не удалось сохранить решение по запросу'));
    }
  };

  const isAuthor = user?.id === ticket.creator.id;
  const isAssignee = user?.id === ticket.assignee?.id;
  const canSeeOverdue = isAgent || isManager || isAdmin;
  const isActive = ['IN_PROGRESS', 'WAITING_USER', 'ANSWERED'].includes(ticket.status);
  const pendingRequest = requests.find((request) => request.status === 'PENDING');

  const canAgentWork =
    isAgent &&
    (ticket.status === 'NEW' ||
      (isAssignee && ['IN_PROGRESS', 'WAITING_USER', 'ANSWERED'].includes(ticket.status)));
  const canAuthorWork = isAuthor && !isAdmin && ticket.status === 'RESOLVED';
  const isClosed = ticket.status === 'CLOSED';
  const canMessage =
    !isClosed && !isAdmin && !isManager && (isAgent ? isAssignee : isAuthor);
  const messageHint = isAdmin
    ? 'Администратор может только просматривать заявки и переписку.'
    : isManager
      ? 'Руководитель может просматривать заявки и принимать решения по запросам.'
      : isAgent && !isAssignee && !isClosed
        ? 'Писать сообщения может только специалист, взявший заявку в работу.'
        : undefined;

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

        {canSeeOverdue && ticket.overdue && (
          <div className="mb-5 flex items-center gap-2 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-red-700">
            <TriangleAlert className="h-4 w-4" />
            Заявка просрочена. Крайний срок: {formatDate(ticket.dueAt)}.
          </div>
        )}

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
            <div className="text-xs text-slate-400">Крайний срок: {formatDate(ticket.dueAt)}</div>
          </div>
        </div>

        {actionError && (
          <p className="mt-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {actionError}
          </p>
        )}

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
                disabled={!reopenReason.trim()}
                className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Вернуть на доработку
              </button>
            </div>
            <input
              type="text"
              value={reopenReason}
              onChange={(event) => setReopenReason(event.target.value)}
              placeholder="Причина возврата на доработку (обязательно)"
              className="mt-3 w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
            />
          </div>
        )}

        {isAgent && isAssignee && isActive && (
          <div className="mt-6 border-t border-slate-100 pt-4">
            <h3 className="mb-3 text-sm font-semibold text-slate-700">
              Запрос руководителю
            </h3>
            {pendingRequest ? (
              <p className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-700">
                Запрос уже отправлен и ожидает решения руководителя.
              </p>
            ) : (
              <div className="flex flex-wrap items-center gap-3">
                <button
                  onClick={() => setRequestOpen(true)}
                  className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100"
                >
                  Запрос руководителю
                </button>
                <span className="text-xs text-slate-400">
                  Отказ от заявки или передача другому специалисту
                </span>
              </div>
            )}
          </div>
        )}

        {requestOpen && !pendingRequest && (
          <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 px-4 py-6"
            onClick={() => !requestSubmitting && setRequestOpen(false)}
          >
            <div
              role="dialog"
              aria-modal="true"
              aria-labelledby="ticket-request-title"
              className="w-full max-w-lg rounded-xl bg-white p-5 shadow-xl"
              onClick={(event) => event.stopPropagation()}
            >
              <div className="mb-4 flex items-start justify-between gap-3">
                <div>
                  <h2 id="ticket-request-title" className="text-base font-semibold text-slate-800">
                    Запрос руководителю
                  </h2>
                  <p className="mt-1 text-sm text-slate-500">
                    Руководитель рассмотрит запрос и примет решение по заявке #{ticket.id}.
                  </p>
                </div>
                <button
                  onClick={() => setRequestOpen(false)}
                  disabled={requestSubmitting}
                  className="rounded-md p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600 disabled:opacity-50"
                  title="Закрыть"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>
              <div className="space-y-3">
                <div className="flex flex-wrap gap-2">
                  <select
                    value={requestType}
                    onChange={(event) => setRequestType(event.target.value as TicketRequestType)}
                    className="rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
                  >
                    <option value="RELEASE">Отказаться от заявки</option>
                    <option value="REASSIGN">Переназначить специалисту</option>
                  </select>
                  {requestType === 'REASSIGN' && (
                    <select
                      value={requestTarget}
                      onChange={(event) => setRequestTarget(event.target.value)}
                      className="rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
                    >
                      <option value="">Выбор за руководителем</option>
                      {agents
                        .filter((agent) => agent.id !== user?.id)
                        .map((agent) => (
                          <option key={agent.id} value={agent.id}>
                            {agent.fullName}
                          </option>
                        ))}
                    </select>
                  )}
                </div>
                <textarea
                  value={requestReason}
                  onChange={(event) => setRequestReason(event.target.value)}
                  rows={4}
                  placeholder="Обоснование для руководителя (обязательно)"
                  className="w-full resize-y rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
                />
                <div className="flex justify-end gap-2">
                  <button
                    onClick={() => setRequestOpen(false)}
                    disabled={requestSubmitting}
                    className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100 disabled:opacity-50"
                  >
                    Отмена
                  </button>
                  <button
                    onClick={handleCreateRequest}
                    disabled={requestSubmitting}
                    className="rounded-md bg-slate-800 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-700 disabled:opacity-50"
                  >
                    {requestSubmitting ? 'Отправка...' : 'Отправить запрос'}
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {canViewRequests && requests.length > 0 && (
          <div className="mt-6 border-t border-slate-100 pt-4">
            <h3 className="mb-3 text-sm font-semibold text-slate-700">Запросы по заявке</h3>
            <TicketRequestList
              requests={requests}
              agents={agents}
              onDecide={isManager || isAdmin ? handleDecide : undefined}
            />
          </div>
        )}

        <MessageThread
          messages={ticket.messages}
          onAddMessage={handleAddMessage}
          canMessage={canMessage}
          messageHint={messageHint}
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
