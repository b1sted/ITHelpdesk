import { useCallback, useEffect, useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Users, Building2, Tags, Plus, Trash2, Pencil, Ticket } from 'lucide-react';
import {
  createCategory,
  createDepartment,
  createUser,
  deleteCategory,
  deleteDepartment,
  deleteUser,
  getDepartments,
  getUsers,
  updateCategory,
  updateDepartment,
  updateUser,
} from '../api/adminApi';
import { getErrorMessage } from '../api/errorMessage';
import { getCategories } from '../api/ticketsApi';
import { useAuth } from '../context/AuthContext';
import type { DepartmentResponse, UserResponse } from '../types/user';
import type { CategoryResponse } from '../types/ticket';
import type { UserRole } from '../types/auth';

type Tab = 'users' | 'departments' | 'categories';

const roleOptions: UserRole[] = ['EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN'];

export function AdminPage() {
  const { user } = useAuth();
  const [tab, setTab] = useState<Tab>('users');
  const [error, setError] = useState('');

  const selectTab = (next: Tab) => {
    setError('');
    setTab(next);
  };

  return (
    <div className="mx-auto max-w-6xl px-4 py-6">
      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="mb-2 text-xl font-semibold text-slate-800">Панель администратора</h1>
          <p className="text-sm text-slate-500">
            Управление справочными данными, сотрудниками и их ролями. Здравствуйте, {user?.fullName}.
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
      <div className="mb-4 flex gap-2">
        <TabButton active={tab === 'users'} onClick={() => selectTab('users')} icon={<Users className="h-4 w-4" />}>
          Пользователи
        </TabButton>
        <TabButton active={tab === 'departments'} onClick={() => selectTab('departments')} icon={<Building2 className="h-4 w-4" />}>
          Департаменты
        </TabButton>
        <TabButton active={tab === 'categories'} onClick={() => selectTab('categories')} icon={<Tags className="h-4 w-4" />}>
          Категории
        </TabButton>
      </div>

      {tab === 'users' && <UsersTab onError={setError} />}
      {tab === 'departments' && <DepartmentsTab onError={setError} />}
      {tab === 'categories' && <CategoriesTab onError={setError} />}
    </div>
  );
}

function TabButton({
  active,
  onClick,
  icon,
  children,
}: {
  active: boolean;
  onClick: () => void;
  icon: ReactNode;
  children: ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition ${
        active ? 'bg-slate-800 text-white' : 'bg-white text-slate-600 hover:bg-slate-100'
      }`}
    >
      {icon}
      {children}
    </button>
  );
}

function UsersTab({ onError }: { onError: (e: string) => void }) {
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editRole, setEditRole] = useState<UserRole>('EMPLOYEE');

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([getUsers(), getDepartments()])
      .then(([u, d]) => {
        onError('');
        setUsers(u);
        setDepartments(d);
      })
      .catch(() => onError('Не удалось загрузить пользователей'))
      .finally(() => setLoading(false));
  }, [onError]);

  useEffect(load, [load]);

  const [form, setForm] = useState({
    fullName: '',
    email: '',
    password: '',
    role: 'EMPLOYEE' as UserRole,
    departmentId: '',
  });

  const handleCreate = async () => {
    if (!form.fullName.trim() || !form.email.trim() || !form.password || !form.departmentId) {
      onError('Заполните все поля нового пользователя');
      return;
    }
    try {
      await createUser({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        password: form.password,
        role: form.role,
        departmentId: Number(form.departmentId),
      });
      setForm({ fullName: '', email: '', password: '', role: 'EMPLOYEE', departmentId: '' });
      load();
    } catch (err: unknown) {
      onError(getErrorMessage(err, 'Не удалось создать пользователя'));
    }
  };

  const handleRoleChange = async (id: number, role: UserRole) => {
    try {
      await updateUser(id, { role });
      load();
    } catch (err: unknown) {
      onError(getErrorMessage(err, 'Не удалось изменить роль'));
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Удалить пользователя?')) return;
    try {
      await deleteUser(id);
      load();
    } catch (err: unknown) {
      onError(getErrorMessage(err, 'Не удалось удалить пользователя'));
    }
  };

  return (
    <div className="rounded-lg bg-white p-5 shadow">
      <div className="mb-4 grid grid-cols-1 gap-3 md:grid-cols-6">
        <input
          className="rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none md:col-span-2"
          placeholder="Полное имя"
          value={form.fullName}
          onChange={(e) => setForm({ ...form, fullName: e.target.value })}
        />
        <input
          className="rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
          placeholder="Email"
          type="email"
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
        />
        <input
          className="rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
          placeholder="Пароль"
          type="password"
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
        />
        <select
          className="rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
          value={form.role}
          onChange={(e) => setForm({ ...form, role: e.target.value as UserRole })}
        >
          {roleOptions.map((r) => (
            <option key={r} value={r}>{r}</option>
          ))}
        </select>
        <select
          className="rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
          value={form.departmentId}
          onChange={(e) => setForm({ ...form, departmentId: e.target.value })}
        >
          <option value="">Департамент</option>
          {departments.map((d) => (
            <option key={d.id} value={d.id}>{d.name}</option>
          ))}
        </select>
      </div>
      <button
        onClick={handleCreate}
        className="mb-4 flex items-center gap-1 rounded-md bg-emerald-600 px-3 py-2 text-sm font-medium text-white transition hover:bg-emerald-700"
      >
        <Plus className="h-4 w-4" />
        Создать пользователя
      </button>

      {loading ? (
        <p className="py-6 text-center text-sm text-slate-400">Загрузка...</p>
      ) : (
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th className="px-3 py-2">Имя</th>
              <th className="px-3 py-2">Email</th>
              <th className="px-3 py-2">Департамент</th>
              <th className="px-3 py-2">Роль</th>
              <th className="px-3 py-2"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {users.map((u) => (
              <tr key={u.id}>
                <td className="px-3 py-2 text-slate-800">{u.fullName}</td>
                <td className="px-3 py-2 text-slate-500">{u.email}</td>
                <td className="px-3 py-2 text-slate-500">{u.departmentName}</td>
                <td className="px-3 py-2">
                  {editingId === u.id ? (
                    <select
                      value={editRole}
                      onChange={(e) => {
                        setEditRole(e.target.value as UserRole);
                      }}
                      className="rounded border border-slate-300 px-2 py-1 text-sm"
                    >
                      {roleOptions.map((r) => (
                        <option key={r} value={r}>{r}</option>
                      ))}
                    </select>
                  ) : (
                    <span className="text-slate-700">{u.role}</span>
                  )}
                </td>
                <td className="px-3 py-2">
                  {editingId === u.id ? (
                    <div className="flex gap-2">
                      <button
                        onClick={async () => {
                          await handleRoleChange(u.id, editRole);
                          setEditingId(null);
                        }}
                        className="rounded bg-emerald-600 px-2 py-1 text-xs text-white hover:bg-emerald-700"
                      >
                        Сохранить
                      </button>
                      <button
                        onClick={() => setEditingId(null)}
                        className="rounded bg-slate-200 px-2 py-1 text-xs text-slate-600"
                      >
                        Отмена
                      </button>
                    </div>
                  ) : (
                    <div className="flex gap-2">
                      <button
                        title="Изменить роль"
                        onClick={() => {
                          setEditingId(u.id);
                          setEditRole(u.role);
                        }}
                        className="rounded p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
                      >
                        <Pencil className="h-4 w-4" />
                      </button>
                      <button
                        title="Удалить"
                        onClick={() => handleDelete(u.id)}
                        className="rounded p-1 text-red-400 hover:bg-red-50 hover:text-red-600"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

function DepartmentsTab({ onError }: { onError: (e: string) => void }) {
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [name, setName] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editName, setEditName] = useState('');

  const load = useCallback(() => {
    getDepartments().then(setDepartments).catch(() => onError('Не удалось загрузить департаменты'));
  }, [onError]);

  useEffect(load, [load]);

  const handleCreate = async () => {
    if (!name.trim()) {
      onError('Укажите название департамента');
      return;
    }
    try {
      await createDepartment(name.trim());
      setName('');
      load();
    } catch (err: unknown) {
      onError(getErrorMessage(err, 'Не удалось создать'));
    }
  };

  const handleSave = async (id: number) => {
    if (!editName.trim()) {
      onError('Название не может быть пустым');
      return;
    }
    try {
      await updateDepartment(id, editName.trim());
      setEditingId(null);
      load();
    } catch (err: unknown) {
      onError(getErrorMessage(err, 'Не удалось изменить'));
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Удалить департамент?')) return;
    try {
      await deleteDepartment(id);
      load();
    } catch (err: unknown) {
      onError(getErrorMessage(err, 'Не удалось удалить'));
    }
  };

  return (
    <div className="rounded-lg bg-white p-5 shadow">
      <div className="mb-4 flex gap-2">
        <input
          className="flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
          placeholder="Название нового департамента"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <button
          onClick={handleCreate}
          className="flex items-center gap-1 rounded-md bg-emerald-600 px-3 py-2 text-sm font-medium text-white hover:bg-emerald-700"
        >
          <Plus className="h-4 w-4" />
          Добавить
        </button>
      </div>
      <table className="w-full text-left text-sm">
        <thead className="bg-slate-50 text-xs uppercase text-slate-500">
          <tr>
            <th className="px-3 py-2">Название</th>
            <th className="px-3 py-2"></th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {departments.map((d) => (
            <tr key={d.id}>
              <td className="px-3 py-2">
                {editingId === d.id ? (
                  <input
                    className="rounded border border-slate-300 px-2 py-1 text-sm"
                    value={editName}
                    onChange={(e) => setEditName(e.target.value)}
                  />
                ) : (
                  <span className="text-slate-800">{d.name}</span>
                )}
              </td>
              <td className="px-3 py-2">
                {editingId === d.id ? (
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleSave(d.id)}
                      className="rounded bg-emerald-600 px-2 py-1 text-xs text-white hover:bg-emerald-700"
                    >
                      Сохранить
                    </button>
                    <button
                      onClick={() => setEditingId(null)}
                      className="rounded bg-slate-200 px-2 py-1 text-xs text-slate-600"
                    >
                      Отмена
                    </button>
                  </div>
                ) : (
                  <div className="flex gap-2">
                    <button
                      title="Изменить"
                      onClick={() => {
                        setEditingId(d.id);
                        setEditName(d.name);
                      }}
                      className="rounded p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
                    >
                      <Pencil className="h-4 w-4" />
                    </button>
                    <button
                      title="Удалить"
                      onClick={() => handleDelete(d.id)}
                      className="rounded p-1 text-red-400 hover:bg-red-50 hover:text-red-600"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function CategoriesTab({ onError }: { onError: (e: string) => void }) {
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [name, setName] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editName, setEditName] = useState('');

  const load = useCallback(() => {
    getCategories().then(setCategories).catch(() => onError('Не удалось загрузить категории'));
  }, [onError]);

  useEffect(load, [load]);

  const handleCreate = async () => {
    if (!name.trim()) {
      onError('Укажите название категории');
      return;
    }
    try {
      await createCategory(name.trim());
      setName('');
      load();
    } catch (err: unknown) {
      onError(getErrorMessage(err, 'Не удалось создать'));
    }
  };

  const handleSave = async (id: number) => {
    if (!editName.trim()) {
      onError('Название не может быть пустым');
      return;
    }
    try {
      await updateCategory(id, editName.trim());
      setEditingId(null);
      load();
    } catch (err: unknown) {
      onError(getErrorMessage(err, 'Не удалось изменить'));
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Удалить категорию?')) return;
    try {
      await deleteCategory(id);
      load();
    } catch (err: unknown) {
      onError(getErrorMessage(err, 'Не удалось удалить'));
    }
  };

  return (
    <div className="rounded-lg bg-white p-5 shadow">
      <div className="mb-4 flex gap-2">
        <input
          className="flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-emerald-500 focus:outline-none"
          placeholder="Название новой категории"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <button
          onClick={handleCreate}
          className="flex items-center gap-1 rounded-md bg-emerald-600 px-3 py-2 text-sm font-medium text-white hover:bg-emerald-700"
        >
          <Plus className="h-4 w-4" />
          Добавить
        </button>
      </div>
      <table className="w-full text-left text-sm">
        <thead className="bg-slate-50 text-xs uppercase text-slate-500">
          <tr>
            <th className="px-3 py-2">Название</th>
            <th className="px-3 py-2"></th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {categories.map((c) => (
            <tr key={c.id}>
              <td className="px-3 py-2">
                {editingId === c.id ? (
                  <input
                    className="rounded border border-slate-300 px-2 py-1 text-sm"
                    value={editName}
                    onChange={(e) => setEditName(e.target.value)}
                  />
                ) : (
                  <span className="text-slate-800">{c.name}</span>
                )}
              </td>
              <td className="px-3 py-2">
                {editingId === c.id ? (
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleSave(c.id)}
                      className="rounded bg-emerald-600 px-2 py-1 text-xs text-white hover:bg-emerald-700"
                    >
                      Сохранить
                    </button>
                    <button
                      onClick={() => setEditingId(null)}
                      className="rounded bg-slate-200 px-2 py-1 text-xs text-slate-600"
                    >
                      Отмена
                    </button>
                  </div>
                ) : (
                  <div className="flex gap-2">
                    <button
                      title="Изменить"
                      onClick={() => {
                        setEditingId(c.id);
                        setEditName(c.name);
                      }}
                      className="rounded p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
                    >
                      <Pencil className="h-4 w-4" />
                    </button>
                    <button
                      title="Удалить"
                      onClick={() => handleDelete(c.id)}
                      className="rounded p-1 text-red-400 hover:bg-red-50 hover:text-red-600"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}