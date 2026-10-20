import { useState, type FormEvent } from 'react';
import { Send } from 'lucide-react';
import type { MessageResponse } from '../types/ticket';

interface MessageThreadProps {
  messages: MessageResponse[];
  onAddMessage: (text: string) => Promise<void>;
  canMessage?: boolean;
}

function formatDate(value: string): string {
  return new Date(value).toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function MessageThread({ messages, onAddMessage, canMessage = true }: MessageThreadProps) {
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
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mt-6">
      <h3 className="mb-3 text-sm font-semibold text-slate-700">Переписка</h3>
      <div className="space-y-3">
        {messages.length === 0 && (
          <p className="text-sm text-slate-400">Сообщений пока нет.</p>
        )}
        {messages.map((message) => (
          <div key={message.id} className="rounded-lg border border-slate-200 bg-white p-3">
            <div className="flex items-baseline justify-between">
              <span className="text-sm font-medium text-slate-800">{message.author.fullName}</span>
              <span className="text-xs text-slate-400">{formatDate(message.createdAt)}</span>
            </div>
            <p className="mt-1 text-sm text-slate-600">{message.text}</p>
          </div>
        ))}
      </div>
      {canMessage && (
        <form onSubmit={handleSubmit} className="mt-4 flex gap-2">
          <input
            type="text"
            value={text}
            onChange={(event) => setText(event.target.value)}
            placeholder="Написать сообщение..."
            className="flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
          />
          <button
            type="submit"
            disabled={submitting || !text.trim()}
            className="flex items-center gap-1 rounded-md bg-emerald-600 px-3 py-2 text-sm font-medium text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <Send className="h-4 w-4" />
            Отправить
          </button>
        </form>
      )}
    </div>
  );
}
