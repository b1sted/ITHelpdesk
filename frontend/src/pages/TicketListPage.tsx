import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Plus, TriangleAlert } from 'lucide-react';
import { getTickets } from '../api/ticketsApi';
import { StatusBadge } from '../components/StatusBadge';
import { useAuth } from '../context/AuthContext';
import type { TicketResponse, TicketStatus } from '../types/ticket';
import { formatDateOnly } from '../utils/formatDate';

type Filter = 'ALL' | 'ACTIVE' | 'ARCHIVE' | TicketStatus;

const agentFilters: { key: Filter; label: string }[] = [
  { key: 'ALL', label: 'Все' },
  { key: 'NEW', label: 'Новые' },
  { key: 'IN_PROGRESS', label: 'В работе' },
  { key: 'WAITING_USER', label: 'Ждёт ответа' },
  { key: 'ANSWERED', label: 'Дан ответ' },
  { key: 'RESOLVED', label: 'Решённые' },
  { key: 'ARCHIVE', label: 'Архив' },
];

const employeeFilters: { key: Filter; label: string }[] = [
  { key: 'ACTIVE', label: 'Активные' },
  { key: 'ARCHIVE', label: 'Архив' },
];

function filterTickets(tickets: TicketResponse[], filter: Filter): TicketResponse[] {
  switch (filter) {
    case 'ACTIVE':
      return tickets.filter((t) => t.status !== 'CLOSED');
    case 'ARCHIVE':
      return tickets.filter((t) => t.status === 'CLOSED');
    default:
      return filter === 'ALL' ? tickets : tickets.filter((t) => t.status === filter);
  }
}

export function TicketListPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const canSeeAllTickets =
    user?.role === 'AGENT' || user?.role === 'MANAGER' || user?.role === 'ADMIN';
  const canSeeOverdue = canSeeAllTickets;
  const [tickets, setTickets] = useState<TicketResponse[]>([]);
  const [filter, setFilter] = useState<Filter>(canSeeAllTickets ? 'ALL' : 'ACTIVE');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const filters = canSeeAllTickets ? agentFilters : employeeFilters;

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getTickets()
      .then((data) => {
        if (!cancelled) {
          setTickets(data);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setError('Не удалось загрузить список заявок');
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
  }, []);

  const filtered = useMemo(
    () => filterTickets(tickets, filter),
    [tickets, filter],
  );

  return (
    <div className="mx-auto max-w-6xl px-4 py-6">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-800">Заявки</h1>
        {user?.role === 'EMPLOYEE' && (
          <Link
            to="/tickets/new"
            className="flex items-center gap-1 rounded-md bg-emerald-600 px-3 py-2 text-sm font-medium text-white transition hover:bg-emerald-700"
          >
            <Plus className="h-4 w-4" />
            Создать заявку
          </Link>
        )}
      </div>

      <div className="mb-4 flex gap-2">
        {filters.map((f) => (
          <button
            key={f.key}
            onClick={() => setFilter(f.key)}
            className={`rounded-full px-4 py-1.5 text-sm font-medium transition ${
              filter === f.key
                ? 'bg-slate-800 text-white'
                : 'bg-white text-slate-600 hover:bg-slate-100'
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {error && <p className="mb-4 text-sm text-red-600">{error}</p>}

      {loading ? (
        <p className="py-8 text-center text-sm text-slate-400">Загрузка...</p>
      ) : filtered.length === 0 ? (
        <p className="py-8 text-center text-sm text-slate-400">Заявок нет.</p>
      ) : (
        <div className="overflow-hidden rounded-lg bg-white shadow">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">№</th>
                <th className="px-4 py-3">Заголовок</th>
                <th className="px-4 py-3">Категория</th>
                <th className="px-4 py-3">Исполнитель</th>
                <th className="px-4 py-3">Статус</th>
                <th className="px-4 py-3">Создана</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filtered.map((ticket) => {
                const highlightOverdue = canSeeOverdue && ticket.overdue;
                return (
                <tr
                  key={ticket.id}
                  onClick={() => navigate(`/tickets/${ticket.id}`)}
                  className={`cursor-pointer transition ${
                    highlightOverdue
                      ? 'bg-red-50 hover:bg-red-100'
                      : 'hover:bg-slate-50'
                  }`}
                >
                  <td className="px-4 py-3 font-medium text-emerald-700">#{ticket.id}</td>
                  <td className="px-4 py-3 text-slate-800">{ticket.title}</td>
                  <td className="px-4 py-3 text-slate-500">{ticket.categoryName}</td>
                  <td className="px-4 py-3 text-slate-500">
                    {ticket.assignee ? ticket.assignee.fullName : 'Не назначен'}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap items-center gap-1">
                      <StatusBadge status={ticket.status} />
                      {highlightOverdue && (
                        <span className="inline-flex items-center gap-1 rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700">
                          <TriangleAlert className="h-3 w-3" />
                          Просрочена
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-slate-500">{formatDateOnly(ticket.createdAt)}</td>
                </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}