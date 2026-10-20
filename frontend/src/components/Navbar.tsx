import { LogOut, Headset, Settings, Ticket } from 'lucide-react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const roleLabels: Record<string, string> = {
  EMPLOYEE: 'Сотрудник',
  AGENT: 'Специалист',
  ADMIN: 'Администратор',
};

function navLinkClass({ isActive }: { isActive: boolean }): string {
  return `flex items-center gap-1 rounded-md px-2 py-1 text-sm transition ${
    isActive ? 'bg-slate-800 text-white' : 'text-slate-300 hover:bg-slate-800 hover:text-white'
  }`;
}

export function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="h-[60px] bg-slate-900 text-white shadow-md">
      <div className="mx-auto flex h-full max-w-6xl items-center justify-between px-4">
        <div className="flex items-center gap-2">
          <Link to="/" className="flex items-center gap-2">
            <Headset className="h-6 w-6 text-emerald-400" />
            <span className="text-lg font-semibold">IT Helpdesk</span>
          </Link>
          <nav className="ml-3 flex items-center gap-1">
            <NavLink to="/" end className={navLinkClass}>
              <Ticket className="h-4 w-4" />
              Заявки
            </NavLink>
            {user?.role === 'ADMIN' && (
              <NavLink to="/admin" className={navLinkClass} title="Панель администратора">
                <Settings className="h-4 w-4" />
                Администрирование
              </NavLink>
            )}
          </nav>
        </div>
        {user && (
          <div className="flex items-center gap-4">
            <div className="text-right">
              <div className="text-sm font-medium leading-tight">{user.fullName}</div>
              <div className="text-xs text-slate-400 leading-tight">{user.email}</div>
            </div>
            <span className="rounded-full bg-emerald-500/20 px-3 py-1 text-xs font-medium text-emerald-300">
              {roleLabels[user.role] ?? user.role}
            </span>
            <button
              onClick={handleLogout}
              className="flex items-center gap-1 rounded-md px-2 py-1 text-sm text-slate-300 transition hover:bg-slate-800 hover:text-white"
              title="Выйти"
            >
              <LogOut className="h-4 w-4" />
              Выйти
            </button>
          </div>
        )}
      </div>
    </header>
  );
}