import { useState, type FormEvent } from 'react';
import { MessageSquare, Send } from 'lucide-react';
import type { MessageResponse } from '../types/ticket';
import { formatDateTime } from '../utils/formatDate';

interface MessageThreadProps {
  messages: MessageResponse[];
  onAddMessage: (text: string) => Promise<void>;
  canMessage?: boolean;
  messageHint?: string;
}

function initials(fullName: string): string {
  return fullName
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase();
}

export function MessageThread({
  messages,
  onAddMessage,
  canMessage = true,
  messageHint,
}: MessageThreadProps) {
  const [text, setText] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!text.trim() || submitting) {
      return;
    }
    setSubmitting(true);
    try {
      await onAddMessage(text);
      setText('');
    } catch {
      // The parent component renders the error message.
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="overflow-hidden rounded-lg bg-white shadow">
      <div className="flex items-center gap-2 border-b border-slate-100 px-5 py-4">
        <MessageSquare className="h-4 w-4 text-slate-400" />
        <h2 className="text-sm font-semibold text-slate-700">Переписка</h2>
        <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-500">
          {messages.length}
        </span>
      </div>

      <div className="space-y-4 px-5 py-4">
        {messages.length === 0 && (
          <p className="text-sm text-slate-400">Сообщений пока нет.</p>
        )}
        {messages.map((message) => (
          <article key={message.id} className="flex gap-3">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-slate-100 text-xs font-semibold text-slate-600">
              {initials(message.author.fullName)}
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-baseline gap-x-2 gap-y-1">
                <span className="text-sm font-medium text-slate-800">
                  {message.author.fullName}
                </span>
                <span className="text-xs text-slate-400">
                  {message.author.departmentName}
                </span>
                <time
                  dateTime={message.createdAt}
                  className="ml-auto text-xs text-slate-400"
                >
                  {formatDateTime(message.createdAt)}
                </time>
              </div>
              <p className="mt-1 whitespace-pre-wrap rounded-lg bg-slate-50 px-3 py-2 text-sm leading-6 text-slate-700">
                {message.text}
              </p>
            </div>
          </article>
        ))}

        {!canMessage && messageHint && (
          <p className="rounded-md bg-slate-50 px-3 py-2 text-sm text-slate-500">
            {messageHint}
          </p>
        )}
      </div>

      {canMessage && (
        <form onSubmit={handleSubmit} className="border-t border-slate-100 p-4">
          <div className="flex items-end gap-2">
            <textarea
              value={text}
              onChange={(event) => setText(event.target.value)}
              rows={2}
              placeholder="Написать сообщение..."
              className="min-h-[42px] flex-1 resize-y rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
            />
            <button
              type="submit"
              disabled={submitting || !text.trim()}
              className="flex items-center gap-1 rounded-md bg-emerald-600 px-3 py-2 text-sm font-medium text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Send className="h-4 w-4" />
              Отправить
            </button>
          </div>
        </form>
      )}
    </section>
  );
}
