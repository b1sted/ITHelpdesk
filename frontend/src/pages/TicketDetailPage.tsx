import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Check, Clock, Pause, Play, TriangleAlert, X } from 'lucide-react';
import { getErrorMessage } from '../api/errorMessage';
import {
  addMessage,
  assignTicket,
  changeTicketStatus,
  extendTicketDeadline,
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
import { TicketHistory } from '../components/TicketHistory';
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
import { formatDateTime } from '../utils/formatDate';

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
  const [deadlineOpen, setDeadlineOpen] = useState(false);
  const [extensionHours, setExtensionHours] = useState('24');
  const [extensionReason, setExtensionReason] = useState('');
  const [deadlineSubmitting, setDeadlineSubmitting] = useState(false);

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

  const handleExtendDeadline = async () => {
    setActionError('');
    const hours = Number(extensionHours);
    if (!Number.isInteger(hours) || hours < 1 || hours > 720) {
      setActionError('Количество часов должно быть целым числом от 1 до 720');
      return;
    }
    if (!extensionReason.trim()) {
      setActionError('Укажите причину продления крайнего срока');
      return;
    }
    setDeadlineSubmitting(true);
    try {
      await extendTicketDeadline(ticket.id, {
        hours,
        reason: extensionReason.trim(),
      });
      setDeadlineOpen(false);
      setExtensionHours('24');
      setExtensionReason('');
      await refresh();
    } catch (err: unknown) {
      setActionError(getErrorMessage(err, 'Не удалось продлить крайний срок'));
    } finally {
      setDeadlineSubmitting(false);
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
  const canExtendDeadline =
    isActive && (isManager || isAdmin || (isAgent && isAssignee));
  const pendingRequests = requests.filter((request) => request.status === 'PENDING');
  const pendingRequest = pendingRequests[0];

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
  const showPendingDecision = (isManager || isAdmin) && pendingRequests.length > 0;
  const showDueInDetails = canSeeOverdue && !canExtendDeadline;

  return (
    <div className="mx-auto max-w-5xl px-4 py-6">
      <button
        onClick={() => navigate('/')}
        className="mb-4 flex items-center gap-1 text-sm text-slate-500 transition hover:text-slate-700"
      >
        <ArrowLeft className="h-4 w-4" />
        К списку заявок
      </button>

      <section className="rounded-lg bg-white p-6 shadow">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-lg font-semibold text-slate-800">#{ticket.id}</span>
          <StatusBadge status={ticket.status} />
          <span className="text-sm text-slate-400">{ticket.categoryName}</span>
        </div>
        <h1 className="mt-3 text-2xl font-semibold text-slate-900">{ticket.title}</h1>
        <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-600">
          {ticket.description}
        </p>

        {canSeeOverdue && ticket.overdue && (
          <div className="mt-5 flex items-center gap-2 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-red-700">
            <TriangleAlert className="h-4 w-4 shrink-0" />
            Заявка просрочена. Крайний срок: {formatDateTime(ticket.dueAt)}.
          </div>
        )}
      </section>

      {actionError && (
        <p className="mt-5 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {actionError}
        </p>
      )}

      {showPendingDecision && (
        <section className="mt-5 rounded-lg border border-amber-200 bg-amber-50 p-5 shadow-sm">
          <div className="mb-3 flex items-start gap-3">
            <Clock className="mt-0.5 h-5 w-5 shrink-0 text-amber-600" />
            <div>
              <h2 className="text-sm font-semibold text-amber-900">
                Запросы, требующие решения
              </h2>
              <p className="mt-1 text-sm text-amber-700">
                Специалист просит освободить его от заявки или передать её другому
                исполнителю.
              </p>
            </div>
          </div>
          <TicketRequestList
            requests={pendingRequests}
            agents={agents}
            onDecide={handleDecide}
          />
        </section>
      )}

      <div className="mt-5 grid gap-5 lg:grid-cols-[minmax(0,1fr)_19rem]">
        <main className="space-y-5">
          {canAgentWork && (
            <section className="rounded-lg bg-white p-5 shadow">
              <h2 className="text-sm font-semibold text-slate-700">Действия по заявке</h2>
              <AgentPanel
                onAssign={handleAssign}
                onStatus={handleStatus}
                status={ticket.status}
              />

              {isAssignee && isActive && (
                <div className="mt-4 border-t border-slate-100 pt-4">
                  {pendingRequest ? (
                    <p className="flex items-start gap-2 rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-700">
                      <Clock className="mt-0.5 h-4 w-4 shrink-0" />
                      Запрос отправлен и ожидает решения руководителя.
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
            </section>
          )}

          {canAuthorWork && (
            <section className="rounded-lg bg-white p-5 shadow">
              <h2 className="text-sm font-semibold text-slate-700">Решение по заявке</h2>
              <p className="mt-1 text-sm text-slate-500">
                Если проблема решена, закройте заявку. Если нет — верните её специалисту.
              </p>
              <label className="mt-4 block text-sm text-slate-600">
                <span className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-400">
                  Причина возврата
                </span>
                <textarea
                  value={reopenReason}
                  onChange={(event) => setReopenReason(event.target.value)}
                  rows={3}
                  placeholder="Опишите, что осталось неисправным (обязательно для возврата)"
                  className="w-full resize-y rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
                />
              </label>
              <div className="mt-3 flex flex-wrap gap-2">
                <button
                  onClick={() => handleStatus('CLOSED')}
                  className="flex items-center gap-1 rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-700"
                >
                  <Check className="h-4 w-4" />
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
            </section>
          )}

          <MessageThread
            messages={ticket.messages}
            onAddMessage={handleAddMessage}
            canMessage={canMessage}
            messageHint={messageHint}
          />

          <TicketHistory
            requests={requests}
            extensions={canSeeOverdue ? ticket.deadlineExtensions : []}
          />
        </main>

        <aside className="space-y-5">
          <section className="rounded-lg bg-white p-5 shadow">
            <h2 className="mb-4 text-sm font-semibold text-slate-700">Сведения</h2>
            <dl className="space-y-4">
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-slate-400">
                  Автор
                </dt>
                <dd className="mt-1 text-sm font-medium text-slate-700">
                  {ticket.creator.fullName}
                </dd>
                <dd className="text-xs text-slate-400">
                  {ticket.creator.departmentName}
                </dd>
              </div>

              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-slate-400">
                  Исполнитель
                </dt>
                {ticket.assignee ? (
                  <>
                    <dd className="mt-1 text-sm font-medium text-slate-700">
                      {ticket.assignee.fullName}
                    </dd>
                    <dd className="text-xs text-slate-400">
                      {ticket.assignee.departmentName}
                    </dd>
                  </>
                ) : (
                  <dd className="mt-1 text-sm text-slate-400">Не назначен</dd>
                )}
              </div>

              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-slate-400">
                  Создана
                </dt>
                <dd className="mt-1 text-sm text-slate-700">
                  {formatDateTime(ticket.createdAt)}
                </dd>
              </div>

              {showDueInDetails && (
                <div>
                  <dt className="text-xs font-medium uppercase tracking-wide text-slate-400">
                    Крайний срок
                  </dt>
                  <dd className="mt-1 text-sm text-slate-700">
                    {formatDateTime(ticket.dueAt)}
                  </dd>
                </div>
              )}

              {ticket.resolvedAt && (
                <div>
                  <dt className="text-xs font-medium uppercase tracking-wide text-slate-400">
                    Решена
                  </dt>
                  <dd className="mt-1 text-sm text-slate-700">
                    {formatDateTime(ticket.resolvedAt)}
                  </dd>
                </div>
              )}

              {ticket.closedAt && (
                <div>
                  <dt className="text-xs font-medium uppercase tracking-wide text-slate-400">
                    Закрыта
                  </dt>
                  <dd className="mt-1 text-sm text-slate-700">
                    {formatDateTime(ticket.closedAt)}
                  </dd>
                </div>
              )}
            </dl>
          </section>

          {canExtendDeadline && (
            <section
              className={`rounded-lg border p-5 shadow-sm ${
                ticket.overdue
                  ? 'border-red-200 bg-red-50'
                  : 'border-slate-200 bg-white'
              }`}
            >
              <div className="flex items-center gap-2">
                <Clock
                  className={`h-4 w-4 ${
                    ticket.overdue ? 'text-red-500' : 'text-slate-400'
                  }`}
                />
                <h2
                  className={`text-sm font-semibold ${
                    ticket.overdue ? 'text-red-800' : 'text-slate-700'
                  }`}
                >
                  Крайний срок
                </h2>
              </div>
              <p
                className={`mt-2 text-sm font-medium ${
                  ticket.overdue ? 'text-red-700' : 'text-slate-700'
                }`}
              >
                {formatDateTime(ticket.dueAt)}
              </p>
              {ticket.extensionHours > 0 && (
                <span className="mt-2 inline-flex rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700">
                  Продлён на {ticket.extensionHours} ч
                </span>
              )}
              <button
                onClick={() => setDeadlineOpen(true)}
                className="mt-3 w-full rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-600 transition hover:bg-slate-100"
              >
                Продлить срок
              </button>
            </section>
          )}
        </aside>
      </div>

      {deadlineOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 px-4 py-6"
          onClick={() => !deadlineSubmitting && setDeadlineOpen(false)}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="deadline-extension-title"
            className="w-full max-w-lg rounded-xl bg-white p-5 shadow-xl"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="mb-4 flex items-start justify-between gap-3">
              <div>
                <h2 id="deadline-extension-title" className="text-base font-semibold text-slate-800">
                  Продлить крайний срок
                </h2>
                <p className="mt-1 text-sm text-slate-500">
                  Текущий срок: {formatDateTime(ticket.dueAt)}. Продление фиксируется в истории заявки.
                </p>
              </div>
              <button
                onClick={() => setDeadlineOpen(false)}
                disabled={deadlineSubmitting}
                className="rounded-md p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600 disabled:opacity-50"
                title="Закрыть"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <div className="space-y-3">
              <label className="block text-sm text-slate-600">
                <span className="mb-1 block text-xs uppercase text-slate-400">
                  Продлить на, часов
                </span>
                <input
                  type="number"
                  min={1}
                  max={720}
                  value={extensionHours}
                  onChange={(event) => setExtensionHours(event.target.value)}
                  className="w-32 rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
                />
              </label>
              <textarea
                value={extensionReason}
                onChange={(event) => setExtensionReason(event.target.value)}
                rows={4}
                placeholder="Причина продления (обязательно)"
                className="w-full resize-y rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
              />
              <div className="flex justify-end gap-2">
                <button
                  onClick={() => setDeadlineOpen(false)}
                  disabled={deadlineSubmitting}
                  className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100 disabled:opacity-50"
                >
                  Отмена
                </button>
                <button
                  onClick={handleExtendDeadline}
                  disabled={deadlineSubmitting}
                  className="rounded-md bg-slate-800 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-700 disabled:opacity-50"
                >
                  {deadlineSubmitting ? 'Сохранение...' : 'Продлить'}
                </button>
              </div>
            </div>
          </div>
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
      <div className="mt-3 flex flex-wrap gap-2">
        <button
          onClick={onAssign}
          className="flex items-center gap-1 rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-700"
        >
          <Play className="h-4 w-4" />
          Взять в работу
        </button>
      </div>
    );
  }

  return (
    <div className="mt-3 flex flex-wrap gap-2">
      {status === 'IN_PROGRESS' && (
        <button
          onClick={() => onStatus?.('WAITING_USER')}
          className="flex items-center gap-1 rounded-md bg-amber-500 px-4 py-2 text-sm font-medium text-white transition hover:bg-amber-600"
        >
          <Pause className="h-4 w-4" />
          Жду ответа
        </button>
      )}
      {status === 'ANSWERED' && (
        <button
          onClick={() => onStatus?.('IN_PROGRESS')}
          className="flex items-center gap-1 rounded-md bg-slate-700 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800"
        >
          <Play className="h-4 w-4" />
          Продолжить работу
        </button>
      )}
      <button
        onClick={() => onStatus?.('RESOLVED')}
        className="flex items-center gap-1 rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-700"
      >
        <Check className="h-4 w-4" />
        Решено
      </button>
    </div>
  );
}
