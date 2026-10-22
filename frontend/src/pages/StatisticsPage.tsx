import { useCallback, useEffect, useState } from 'react';
import { BarChart3, Ticket, TriangleAlert } from 'lucide-react';
import { Link } from 'react-router-dom';
import { getErrorMessage } from '../api/errorMessage';
import { getAgentStatistics } from '../api/statisticsApi';
import { updateAgentTicketLimit } from '../api/adminApi';
import {
  decideTicketRequest,
  getTicketRequestsByStatus,
} from '../api/ticketRequestsApi';
import { TicketRequestList } from '../components/TicketRequestList';
import type {
  AgentStatisticsResponse,
  TicketRequestResponse,
  TicketRequestStatus,
} from '../types/ticket';

export function StatisticsPage() {
  const [statistics, setStatistics] = useState<AgentStatisticsResponse[]>([]);
  const [requests, setRequests] = useState<TicketRequestResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([getAgentStatistics(), getTicketRequestsByStatus('PENDING')])
      .then(([stats, pending]) => {
        setError('');
        setStatistics(stats);
        setRequests(pending);
      })
      .catch(() => setError('Не удалось загрузить статистику'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(load, [load]);

  const handleDecide = async (
    requestId: number,
    status: Exclude<TicketRequestStatus, 'PENDING'>,
    comment?: string,
  ) => {
    try {
      await decideTicketRequest(requestId, { status, comment });
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
            <h2 className="mb-3 text-sm font-semibold text-slate-700">Специалисты поддержки</h2>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                  <tr>
                    <th className="px-3 py-2">Специалист</th>
                    <th className="px-3 py-2">Департамент</th>
                    <th className="px-3 py-2">Лимит</th>
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
                        <LimitEditor agent={agent} onSaved={load} onError={setError} />
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
                      <td colSpan={8} className="px-3 py-6 text-center text-sm text-slate-400">
                        Специалистов поддержки пока нет.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>

          <section className="rounded-lg bg-white p-5 shadow">
            <h2 className="mb-3 text-sm font-semibold text-slate-700">
              Запросы, ожидающие решения
            </h2>
            <TicketRequestList
              requests={requests}
              showTicket
              onDecide={handleDecide}
            />
          </section>
        </div>
      )}
    </div>
  );
}

function LimitEditor({
  agent,
  onSaved,
  onError,
}: {
  agent: AgentStatisticsResponse;
  onSaved: () => void;
  onError: (message: string) => void;
}) {
  const [value, setValue] = useState(String(agent.ticketLimit ?? 10));
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setValue(String(agent.ticketLimit ?? 10));
  }, [agent.ticketLimit]);

  const save = async () => {
    const limit = Number(value);
    if (!Number.isInteger(limit) || limit < 1) {
      onError('Предел заявок должен быть целым числом не менее 1');
      return;
    }
    setSaving(true);
    try {
      await updateAgentTicketLimit(agent.agentId, limit);
      onError('');
      onSaved();
    } catch (err: unknown) {
      onError(getErrorMessage(err, 'Не удалось изменить предел заявок'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="flex items-center gap-1">
      <input
        type="number"
        min={1}
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
