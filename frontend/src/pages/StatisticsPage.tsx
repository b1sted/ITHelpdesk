import { useCallback, useEffect, useState } from 'react';
import { BarChart3, Ticket, TriangleAlert } from 'lucide-react';
import { Link } from 'react-router-dom';
import { getErrorMessage } from '../api/errorMessage';
import {
  getAgentStatistics,
  getPeriodStatistics,
  getSupportSettings,
  updateSupportSettings,
} from '../api/statisticsApi';
import { getAgents, updateAgentLimits } from '../api/adminApi';
import {
  decideTicketRequest,
  getTicketRequestsByStatus,
} from '../api/ticketRequestsApi';
import { TicketRequestList } from '../components/TicketRequestList';
import type {
  AgentStatisticsResponse,
  PeriodStatisticsResponse,
  StatisticsGranularity,
  TicketRequestResponse,
  TicketRequestStatus,
} from '../types/ticket';
import type { UserResponse } from '../types/user';

export function StatisticsPage() {
  const [statistics, setStatistics] = useState<AgentStatisticsResponse[]>([]);
  const [requests, setRequests] = useState<TicketRequestResponse[]>([]);
  const [agents, setAgents] = useState<UserResponse[]>([]);
  const [autoCloseHours, setAutoCloseHours] = useState('0');
  const [autoCloseSaving, setAutoCloseSaving] = useState(false);
  const [granularity, setGranularity] = useState<StatisticsGranularity>('MONTH');
  const [period, setPeriod] = useState<PeriodStatisticsResponse | null>(null);
  const [periodLoading, setPeriodLoading] = useState(true);
  const [requestStatus, setRequestStatus] = useState<TicketRequestStatus | 'ALL'>('PENDING');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([
      getAgentStatistics(),
      getTicketRequestsByStatus(requestStatus === 'ALL' ? undefined : requestStatus),
      getAgents(),
      getSupportSettings(),
    ])
      .then(([stats, requestList, agentList, settings]) => {
        setError('');
        setStatistics(stats);
        setRequests(requestList);
        setAgents(agentList);
        setAutoCloseHours(String(settings.autoCloseResolvedHours));
      })
      .catch(() => setError('Не удалось загрузить статистику'))
      .finally(() => setLoading(false));
  }, [requestStatus]);

  useEffect(load, [load]);

  const loadPeriod = useCallback(() => {
    setPeriodLoading(true);
    getPeriodStatistics(granularity)
      .then((data) => {
        setPeriod(data);
        setError('');
      })
      .catch(() => setError('Не удалось загрузить статистику за период'))
      .finally(() => setPeriodLoading(false));
  }, [granularity]);

  useEffect(loadPeriod, [loadPeriod]);

  const handleSaveAutoClose = async () => {
    const hours = Number(autoCloseHours);
    if (!Number.isInteger(hours) || hours < 0 || hours > 8760) {
      setError('Срок автозакрытия должен быть целым числом от 0 до 8760 часов');
      return;
    }
    setAutoCloseSaving(true);
    try {
      const settings = await updateSupportSettings(hours);
      setAutoCloseHours(String(settings.autoCloseResolvedHours));
      setError('');
    } catch (err: unknown) {
      setError(getErrorMessage(err, 'Не удалось сохранить настройки автозакрытия'));
    } finally {
      setAutoCloseSaving(false);
    }
  };

  const handleDecide = async (
    requestId: number,
    status: Exclude<TicketRequestStatus, 'PENDING'>,
    comment?: string,
    targetAssigneeId?: number,
  ) => {
    try {
      await decideTicketRequest(requestId, { status, comment, targetAssigneeId });
      load();
    } catch (err: unknown) {
      setError(getErrorMessage(err, 'Не удалось сохранить решение по запросу'));
    }
  };

  return (
    <div className="mx-auto max-w-6xl px-4 py-6">
      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="mb-2 flex items-center gap-2 text-xl font-semibold text-slate-800">
            <BarChart3 className="h-5 w-5 text-emerald-600" />
            Статистика поддержки
          </h1>
          <p className="text-sm text-slate-500">
            Нагрузка специалистов, просроченные заявки и запросы на отказ или переназначение.
          </p>
        </div>
        <Link
          to="/"
          className="flex items-center gap-1 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100"
        >
          <Ticket className="h-4 w-4" />
          К заявкам
        </Link>
      </div>

      {error && (
        <p className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}

      {loading ? (
        <p className="py-8 text-center text-sm text-slate-400">Загрузка...</p>
      ) : (
        <div className="space-y-6">
          <section className="rounded-lg bg-white p-5 shadow">
            <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
              <div>
                <h2 className="text-sm font-semibold text-slate-700">Динамика заявок</h2>
                <p className="mt-1 text-xs text-slate-500">
                  Созданные, решённые и закрытые заявки по месяцам текущего года или по годам.
                </p>
              </div>
              <div className="flex gap-1 rounded-lg bg-slate-100 p-1">
                <button
                  onClick={() => setGranularity('MONTH')}
                  className={`rounded-md px-3 py-1.5 text-xs font-medium transition ${
                    granularity === 'MONTH'
                      ? 'bg-white text-slate-800 shadow'
                      : 'text-slate-500 hover:text-slate-700'
                  }`}
                >
                  По месяцам
                </button>
                <button
                  onClick={() => setGranularity('YEAR')}
                  className={`rounded-md px-3 py-1.5 text-xs font-medium transition ${
                    granularity === 'YEAR'
                      ? 'bg-white text-slate-800 shadow'
                      : 'text-slate-500 hover:text-slate-700'
                  }`}
                >
                  По годам
                </button>
              </div>
            </div>
            {periodLoading || !period ? (
              <p className="py-8 text-center text-sm text-slate-400">Загрузка...</p>
            ) : (
              <PeriodChart data={period} />
            )}
          </section>

          <section className="rounded-lg bg-white p-5 shadow">
            <h2 className="mb-1 text-sm font-semibold text-slate-700">
              Автозакрытие решённых заявок
            </h2>
            <p className="mb-3 text-xs text-slate-500">
              Заявка в статусе «Решена» будет закрыта автоматически, если сотрудник не подтвердил
              решение. Укажите 0, чтобы отключить автозакрытие.
            </p>
            <div className="flex flex-wrap items-end gap-3">
              <label className="text-sm text-slate-600">
                <span className="mb-1 block text-xs uppercase text-slate-400">Срок, часов</span>
                <input
                  type="number"
                  min={0}
                  max={8760}
                  value={autoCloseHours}
                  onChange={(event) => setAutoCloseHours(event.target.value)}
                  className="w-32 rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
                />
              </label>
              <button
                onClick={handleSaveAutoClose}
                disabled={autoCloseSaving}
                className="rounded-md bg-slate-800 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-700 disabled:opacity-50"
              >
                {autoCloseSaving ? 'Сохранение...' : 'Сохранить'}
              </button>
            </div>
          </section>

          <section className="rounded-lg bg-white p-5 shadow">
            <h2 className="mb-3 text-sm font-semibold text-slate-700">Специалисты поддержки</h2>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                  <tr>
                    <th className="px-3 py-2">Специалист</th>
                    <th className="px-3 py-2">Департамент</th>
                    <th className="px-3 py-2">Лимит заявок</th>
                    <th className="px-3 py-2">Лимит времени, ч</th>
                    <th className="px-3 py-2">Активные</th>
                    <th className="px-3 py-2">Решённые</th>
                    <th className="px-3 py-2">Закрытые</th>
                    <th className="px-3 py-2">Просроченные</th>
                    <th className="px-3 py-2">Всего</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {statistics.map((agent) => (
                    <tr key={agent.agentId}>
                      <td className="px-3 py-2">
                        <div className="font-medium text-slate-800">{agent.fullName}</div>
                        <div className="text-xs text-slate-400">{agent.email}</div>
                      </td>
                      <td className="px-3 py-2 text-slate-500">{agent.departmentName}</td>
                      <td className="px-3 py-2">
                        <LimitEditor field="ticketLimit" agent={agent} onSaved={load} onError={setError} />
                      </td>
                      <td className="px-3 py-2">
                        <LimitEditor field="timeLimitHours" agent={agent} onSaved={load} onError={setError} />
                      </td>
                      <td className="px-3 py-2 text-slate-700">{agent.activeTickets}</td>
                      <td className="px-3 py-2 text-slate-700">{agent.resolvedTickets}</td>
                      <td className="px-3 py-2 text-slate-700">{agent.closedTickets}</td>
                      <td className="px-3 py-2">
                        <span
                          className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${
                            agent.overdueTickets > 0
                              ? 'bg-red-100 text-red-700'
                              : 'bg-slate-100 text-slate-500'
                          }`}
                        >
                          {agent.overdueTickets > 0 && <TriangleAlert className="h-3 w-3" />}
                          {agent.overdueTickets}
                        </span>
                      </td>
                      <td className="px-3 py-2 text-slate-700">{agent.totalAssigned}</td>
                    </tr>
                  ))}
                  {statistics.length === 0 && (
                    <tr>
                      <td colSpan={9} className="px-3 py-6 text-center text-sm text-slate-400">
                        Специалистов поддержки пока нет.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>

          <section className="rounded-lg bg-white p-5 shadow">
            <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-sm font-semibold text-slate-700">
                Запросы на отказ и переназначение
              </h2>
              <div className="flex gap-1 rounded-lg bg-slate-100 p-1">
                <button
                  onClick={() => setRequestStatus('PENDING')}
                  className={`rounded-md px-3 py-1.5 text-xs font-medium transition ${
                    requestStatus === 'PENDING'
                      ? 'bg-white text-slate-800 shadow'
                      : 'text-slate-500 hover:text-slate-700'
                  }`}
                >
                  Ожидают решения
                </button>
                <button
                  onClick={() => setRequestStatus('ALL')}
                  className={`rounded-md px-3 py-1.5 text-xs font-medium transition ${
                    requestStatus === 'ALL'
                      ? 'bg-white text-slate-800 shadow'
                      : 'text-slate-500 hover:text-slate-700'
                  }`}
                >
                  Вся история
                </button>
              </div>
            </div>
            <TicketRequestList
              requests={requests}
              agents={agents}
              showTicket
              onDecide={handleDecide}
            />
          </section>
        </div>
      )}
    </div>
  );
}

function PeriodChart({ data }: { data: PeriodStatisticsResponse }) {
  const max = Math.max(
    1,
    ...data.buckets.flatMap((bucket) => [bucket.created, bucket.resolved, bucket.closed]),
  );

  return (
    <div>
      <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <Metric label="Создано" value={data.totalCreated} accent="text-emerald-600" />
        <Metric label="Решено" value={data.totalResolved} accent="text-sky-600" />
        <Metric label="Закрыто" value={data.totalClosed} accent="text-slate-700" />
        <Metric
          label="Среднее решение"
          value={`${data.averageResolutionHours} ч`}
          accent="text-amber-600"
        />
      </div>

      <div className="overflow-x-auto">
        <div className="flex h-56 min-w-[520px] items-end gap-2">
          {data.buckets.map((bucket) => (
            <div key={bucket.label} className="flex flex-1 flex-col items-center gap-2">
              <div className="flex h-44 w-full items-end justify-center gap-1">
                <Bar
                  value={bucket.created}
                  max={max}
                  label={`${bucket.label}: создано ${bucket.created}`}
                  color="bg-emerald-500"
                />
                <Bar
                  value={bucket.resolved}
                  max={max}
                  label={`${bucket.label}: решено ${bucket.resolved}`}
                  color="bg-sky-500"
                />
                <Bar
                  value={bucket.closed}
                  max={max}
                  label={`${bucket.label}: закрыто ${bucket.closed}`}
                  color="bg-slate-500"
                />
              </div>
              <span className="text-[10px] text-slate-500">{bucket.label}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="mt-4 flex flex-wrap gap-4 text-xs text-slate-500">
        <Legend color="bg-emerald-500" label="Создано" />
        <Legend color="bg-sky-500" label="Решено" />
        <Legend color="bg-slate-500" label="Закрыто" />
      </div>
    </div>
  );
}

function Metric({
  label,
  value,
  accent,
}: {
  label: string;
  value: number | string;
  accent: string;
}) {
  return (
    <div className="rounded-lg bg-slate-50 px-3 py-2">
      <div className="text-xs uppercase text-slate-400">{label}</div>
      <div className={`text-xl font-semibold ${accent}`}>{value}</div>
    </div>
  );
}

function Bar({
  value,
  max,
  label,
  color,
}: {
  value: number;
  max: number;
  label: string;
  color: string;
}) {
  const height = value === 0 ? 0 : Math.max(4, Math.round((value / max) * 100));
  return (
    <div
      title={label}
      style={{ height: `${height}%` }}
      className={`w-2.5 rounded-t ${color}`}
    />
  );
}

function Legend({ color, label }: { color: string; label: string }) {
  return (
    <span className="flex items-center gap-1">
      <span className={`h-2 w-2 rounded-full ${color}`} />
      {label}
    </span>
  );
}

function LimitEditor({
  field,
  agent,
  onSaved,
  onError,
}: {
  field: 'ticketLimit' | 'timeLimitHours';
  agent: AgentStatisticsResponse;
  onSaved: () => void;
  onError: (message: string) => void;
}) {
  const fallback = field === 'ticketLimit' ? 10 : 24;
  const [value, setValue] = useState(String(agent[field] ?? fallback));
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setValue(String(agent[field] ?? fallback));
  }, [agent, field, fallback]);

  const save = async () => {
    const limit = Number(value);
    if (!Number.isInteger(limit) || limit < 1) {
      onError('Значение лимита должно быть целым числом не менее 1');
      return;
    }
    if (field === 'timeLimitHours' && limit > 720) {
      onError('Лимит времени не может превышать 720 часов');
      return;
    }
    setSaving(true);
    try {
      await updateAgentLimits(agent.agentId, {
        ticketLimit: field === 'ticketLimit' ? limit : agent.ticketLimit ?? 10,
        timeLimitHours: field === 'timeLimitHours' ? limit : agent.timeLimitHours ?? 24,
      });
      onError('');
      onSaved();
    } catch (err: unknown) {
      onError(getErrorMessage(err, 'Не удалось изменить лимиты специалиста'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="flex items-center gap-1">
      <input
        type="number"
        min={1}
        max={field === 'timeLimitHours' ? 720 : 1000}
        value={value}
        onChange={(event) => setValue(event.target.value)}
        className="w-16 rounded-md border border-slate-300 px-2 py-1 text-sm focus:border-emerald-500 focus:outline-none"
      />
      <button
        onClick={save}
        disabled={saving}
        className="rounded bg-slate-800 px-2 py-1 text-xs font-medium text-white transition hover:bg-slate-700 disabled:opacity-50"
      >
        ОК
      </button>
    </div>
  );
}
