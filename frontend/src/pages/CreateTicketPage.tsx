import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { getErrorMessage } from '../api/errorMessage';
import { createTicket, getCategories } from '../api/ticketsApi';
import type { CategoryResponse } from '../types/ticket';

export function CreateTicketPage() {
  const navigate = useNavigate();
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getCategories()
      .then(setCategories)
      .catch(() => setError('Не удалось загрузить категории'));
  }, []);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    if (!title.trim() || !description.trim() || !categoryId) {
      setError('Все поля обязательны для заполнения');
      return;
    }
    setSubmitting(true);
    try {
      const ticket = await createTicket({
        title: title.trim(),
        description: description.trim(),
        categoryId: Number(categoryId),
      });
      navigate(`/tickets/${ticket.id}`);
    } catch (err: unknown) {
      setError(getErrorMessage(err, 'Не удалось создать заявку'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mx-auto max-w-2xl px-4 py-6">
      <button
        onClick={() => navigate('/')}
        className="mb-4 flex items-center gap-1 text-sm text-slate-500 transition hover:text-slate-700"
      >
        <ArrowLeft className="h-4 w-4" />
        К списку заявок
      </button>
      <h1 className="mb-6 text-xl font-semibold text-slate-800">Новая заявка</h1>

      {error && (
        <p className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}

      <form onSubmit={handleSubmit} className="space-y-4 rounded-lg bg-white p-6 shadow">
        <div>
          <label htmlFor="category" className="mb-1 block text-sm font-medium text-slate-700">
            Категория
          </label>
          <select
            id="category"
            value={categoryId}
            onChange={(event) => setCategoryId(event.target.value)}
            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
          >
            <option value="">Выберите категорию</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="title" className="mb-1 block text-sm font-medium text-slate-700">
            Заголовок
          </label>
          <input
            id="title"
            type="text"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
            placeholder="Краткое описание проблемы"
          />
        </div>
        <div>
          <label htmlFor="description" className="mb-1 block text-sm font-medium text-slate-700">
            Подробное описание
          </label>
          <textarea
            id="description"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            rows={6}
            className="w-full resize-y rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
            placeholder="Опишите проблему подробнее..."
          />
        </div>
        <div className="flex gap-3">
          <button
            type="submit"
            disabled={submitting}
            className="rounded-md bg-emerald-600 px-5 py-2 text-sm font-medium text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {submitting ? 'Создание...' : 'Создать заявку'}
          </button>
          <button
            type="button"
            onClick={() => navigate('/')}
            className="rounded-md border border-slate-300 px-5 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100"
          >
            Отмена
          </button>
        </div>
      </form>
    </div>
  );
}