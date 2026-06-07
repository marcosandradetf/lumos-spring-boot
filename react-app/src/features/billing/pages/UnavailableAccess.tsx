import { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import { useAppStore } from '@/store/use-app-store';
import { useAuthStore } from '@/core/auth/useAuthStore';

export default function UnavailableAccess() {
  const navigate = useNavigate();
  const location = useLocation();
  const { logout, isLoading } = useAuthStore();
  const { setPageContext } = useAppStore();

  useEffect(() => {
    setPageContext(['Acesso indisponível'], 'Acesso indisponível');

    const isLocked = localStorage.getItem('isLocked') !== null;
    if (!isLocked) {
      navigate('/dashboard', { replace: true });
    }
  }, [navigate, setPageContext]);

  return (
    <section className="relative flex min-h-[calc(100vh-52px)] items-center justify-center overflow-hidden bg-slate-950 px-4 py-8">
      <div className="absolute -left-20 top-10 h-72 w-72 rounded-full bg-blue-500/20 blur-3xl" />
      <div className="absolute -right-20 bottom-10 h-72 w-72 rounded-full bg-cyan-400/20 blur-3xl" />

      <article className="relative z-10 w-full max-w-2xl rounded-3xl border border-white/10 bg-slate-900/70 p-8 text-center shadow-2xl backdrop-blur-xl">
        <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl border border-white/15 bg-white/5">
          <i className="pi pi-lock text-xl text-amber-300" />
        </div>

        <p className="text-xs font-semibold uppercase tracking-[0.24em] text-cyan-200">Acesso indisponível</p>
        <h1 className="mt-2 text-3xl font-semibold text-white">Acesso temporariamente indisponível</h1>
        <p className="mt-4 text-sm leading-7 text-slate-300">
          No momento, não foi possível liberar seu acesso ao sistema. Em caso de dúvidas,
          fale com um usuário administrador.
        </p>

        <div className="mt-6 grid gap-3 sm:grid-cols-2">
          <button
            type="button"
            onClick={() => navigate(location.state?.from?.pathname ?? '/dashboard')}
            className="rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-4 py-2.5 text-sm font-semibold text-white hover:from-blue-500 hover:to-cyan-400"
          >
            <i className="pi pi-replay mr-2" />Tentar novamente
          </button>

          <button
            type="button"
            onClick={() => void logout()}
            disabled={isLoading}
            className="rounded-xl border border-white/20 px-4 py-2.5 text-sm font-semibold text-white hover:bg-white/10 disabled:opacity-50"
          >
            {isLoading ? <i className="pi pi-spin pi-spinner mr-2" /> : <i className="pi pi-sign-out mr-2" />}Fazer logout
          </button>
        </div>

        <a
          href="mailto:support@lumos.com?subject=Suporte%20Lumos&body=Ol%C3%A1%2C%20preciso%20de%20suporte%20para%20acessar%20minha%20conta."
          className="mt-6 inline-flex text-sm font-medium text-cyan-200 hover:text-cyan-100"
        >
          Precisa de ajuda? Contate nosso suporte.
        </a>
      </article>
    </section>
  );
}
