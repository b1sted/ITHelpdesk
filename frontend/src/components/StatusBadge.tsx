import type { TicketStatus } from '../types/auth';

const statusConfig: Record<TicketStatus, { label: string; classes: string }> = {
  NEW: { label: 'Новая', classes: 'bg-blue-100 text-blue-700' },
  IN_PROGRESS: { label: 'В работе', classes: 'bg-amber-100 text-amber-700' },
  WAITING_USER: { label: 'Ждёт ответа', classes: 'bg-purple-100 text-purple-700' },
  RESOLVED: { label: 'Решена', classes: 'bg-emerald-100 text-emerald-700' },
  CLOSED: { label: 'Закрыта', classes: 'bg-slate-200 text-slate-600' },
};

export function StatusBadge({ status }: { status: TicketStatus }) {
  const config = statusConfig[status];
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${config.classes}`}
    >
      {config.label}
    </span>
  );
}