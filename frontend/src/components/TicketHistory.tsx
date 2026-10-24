import { useState } from 'react';
import { ChevronDown, History } from 'lucide-react';
import type {
  TicketClosureResponse,
  TicketDeadlineExtensionResponse,
  TicketRequestResponse,
} from '../types/ticket';
import { formatDateTime } from '../utils/formatDate';
import { TicketRequestList } from './TicketRequestList';

interface TicketHistoryProps {
  requests: TicketRequestResponse[];
  extensions: TicketDeadlineExtensionResponse[];
  closure?: TicketClosureResponse | null;
}

export function TicketHistory({ requests, extensions, closure }: TicketHistoryProps) {
  const [open, setOpen] = useState(false);
  const decidedRequests = requests.filter((request) => request.status !== 'PENDING');
  const total = decidedRequests.length + extensions.length + (closure ? 1 : 0);

  if (total === 0) {
    return null;
  }

  return (
    <section className="overflow-hidden rounded-lg bg-white shadow">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        aria-expanded={open}
        className="flex w-full items-center gap-2 px-5 py-4 text-left transition hover:bg-slate-50"
      >
        <History className="h-4 w-4 text-slate-400" />
        <span className="text-sm font-semibold text-slate-700">История заявки</span>
        <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-500">
          {total}
        </span>
        <ChevronDown
          className={`ml-auto h-4 w-4 text-slate-400 transition-transform ${
            open ? 'rotate-180' : ''
          }`}
        />
      </button>

      {open && (
        <div className="space-y-5 border-t border-slate-100 px-5 py-4">
          {decidedRequests.length > 0 && (
            <div>
              <h3 className="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
                Запросы по заявке
              </h3>
              <TicketRequestList requests={decidedRequests} />
            </div>
          )}

          {closure && (
            <div>
              <h3 className="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
                Закрытие заявки
              </h3>
              <div className="rounded-lg border border-slate-200 bg-white p-3">
                <div className="flex flex-wrap items-baseline justify-between gap-2">
                  <span className="text-sm font-medium text-slate-800">
                    Закрыта специалистом
                  </span>
                  <span className="text-xs text-slate-400">
                    {formatDateTime(closure.createdAt)}
                  </span>
                </div>
                <p className="mt-1 text-sm text-slate-600">{closure.reason}</p>
                <p className="mt-1 text-xs text-slate-400">{closure.closedBy.fullName}</p>
              </div>
            </div>
          )}

          {extensions.length > 0 && (
            <div>
              <h3 className="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
                Продление срока
              </h3>
              <ol className="space-y-3">
                {extensions.map((extension) => (
                  <li
                    key={extension.id}
                    className="rounded-lg border border-slate-200 bg-white p-3"
                  >
                    <div className="flex flex-wrap items-baseline justify-between gap-2">
                      <span className="text-sm font-medium text-slate-800">
                        +{extension.hours} ч
                      </span>
                      <span className="text-xs text-slate-400">
                        {formatDateTime(extension.createdAt)}
                      </span>
                    </div>
                    <p className="mt-1 text-sm text-slate-600">{extension.reason}</p>
                    <p className="mt-1 text-xs text-slate-400">
                      {extension.createdBy.fullName}
                    </p>
                  </li>
                ))}
              </ol>
            </div>
          )}
        </div>
      )}
    </section>
  );
}
