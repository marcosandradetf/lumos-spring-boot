import { createPortal } from 'react-dom';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../core/auth/useAuthStore';
import { useAppStore } from '../../store/use-app-store';

export function AccountDrawer() {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const { showAccountDrawer, setShowAccountDrawer } = useAppStore();

  const initial = user?.fullName?.charAt(0)?.toUpperCase() || 'U';

  const onNavigate = (path: string) => {
    navigate(path);
    setShowAccountDrawer(false);
  };

  const onLogout = async () => {
    await logout();
    setShowAccountDrawer(false);
  };

  return createPortal(
    <>
      <div
        className={[
          'fixed inset-0 z-[300] bg-black/40 transition-opacity duration-300',
          showAccountDrawer ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none',
        ].join(' ')}
        onClick={() => setShowAccountDrawer(false)}
      />

      <aside
        className={[
          'fixed right-0 top-0 z-[310] h-screen w-full max-w-[20rem] transform border-l border-white/10 bg-white dark:bg-zinc-950 shadow-2xl transition-transform duration-300',
          showAccountDrawer ? 'translate-x-0' : 'translate-x-full',
        ].join(' ')}
        aria-hidden={!showAccountDrawer}
      >
        <div className="flex h-full flex-col">
          <header className="flex items-center gap-3 border-b border-slate-200 px-4 py-4 dark:border-zinc-800">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-blue-600 to-cyan-500 text-sm font-semibold text-white">
              {initial}
            </div>
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-slate-800 dark:text-zinc-100">{user?.fullName}</p>
              <p className="truncate text-xs text-slate-500 dark:text-zinc-400">{user?.username}</p>
            </div>
          </header>

          <div className="flex-1 py-2">
            <p className="px-4 py-2 text-[11px] uppercase tracking-[0.14em] text-slate-400">Conta</p>
            <button
              type="button"
              onClick={() => onNavigate('/configuracoes/conta')}
              className="flex w-full items-center gap-3 px-4 py-3 text-sm text-slate-700 transition-colors hover:bg-slate-100 dark:text-zinc-200 dark:hover:bg-zinc-900"
            >
              <i className="pi pi-user text-xs" />
              Perfil
            </button>

            <button
              type="button"
              onClick={() => onNavigate('/configuracoes')}
              className="flex w-full items-center gap-3 px-4 py-3 text-sm text-slate-700 transition-colors hover:bg-slate-100 dark:text-zinc-200 dark:hover:bg-zinc-900"
            >
              <i className="pi pi-cog text-xs" />
              Configurações
            </button>

            <div className="my-2 border-t border-slate-200 dark:border-zinc-800" />

            <p className="px-4 py-2 text-[11px] uppercase tracking-[0.14em] text-slate-400">Segurança</p>
            <button
              type="button"
              onClick={onLogout}
              className="flex w-full items-center gap-3 px-4 py-3 text-sm text-red-600 transition-colors hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-950/30"
            >
              <i className="pi pi-sign-out text-xs" />
              Sair
            </button>
          </div>

          <footer className="border-t border-slate-200 px-4 py-3 text-[11px] text-slate-400 dark:border-zinc-800">
            Lumos © 2026
          </footer>
        </div>
      </aside>
    </>,
    document.body,
  );
}
