import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import api from '../../../core/auth/api';
import { useAuthStore } from '../../../core/auth/useAuthStore';
import { useAppStore } from '../../../store/use-app-store';
import { checkOnboardingState } from '../../../core/onboarding/checkOnboardingState';

export default function AutoLogin() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const { applyAuthResponse } = useAuthStore();
  const { toggleMenu, setOnboarding } = useAppStore();

  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const redirectPath = useMemo(() => {
    const redirectFromQuery = searchParams.get('redirect');
    if (redirectFromQuery) {
      return redirectFromQuery;
    }

    const from = (location.state as { from?: { pathname?: string; search?: string } } | null)?.from;
    if (from?.pathname) {
      return `${from.pathname}${from.search ?? ''}`;
    }

    return '/';
  }, [location.state, searchParams]);

  useEffect(() => {
    let mounted = true;

    const run = async () => {
      try {
        const response = await api.post<{ accessToken: string }>('/api/auth/refresh-token', {});

        if (!mounted) return;
        applyAuthResponse(response.data.accessToken);

        const authUser = useAuthStore.getState().user;
        const roles = authUser?.roles ?? [];
        const hasPermission = roles.includes('ADMIN') || roles.includes('ANALISTA') || roles.includes('RESPONSAVEL_TECNICO');

        localStorage.setItem('isSupport', authUser?.support ? 'true' : 'false');

        if (window.matchMedia('(min-width: 1280px)').matches) {
          localStorage.setItem('menuOpen', 'true');
          toggleMenu(true);
        }

        if (localStorage.getItem('onboarding') || !hasPermission) {
          setLoading(false);
          navigate(redirectPath, { replace: true });
          return;
        }

        const isOnboardingPending = await checkOnboardingState();
        if (!mounted) return;

        if (isOnboardingPending) {
          localStorage.removeItem('onboarding');
          setOnboarding(true);
          navigate('/configuracoes/onboarding', { replace: true });
        } else {
          localStorage.setItem('onboarding', 'finished');
          setOnboarding(false);
          navigate(redirectPath, { replace: true });
        }
      } catch {
        if (!mounted) return;

        setLoading(false);
        setErrorMessage('Nao foi possivel autenticar automaticamente. Redirecionando para login...');
        localStorage.removeItem('menuOpen');

        window.setTimeout(() => {
          navigate(`/auth/login?redirect=${encodeURIComponent(redirectPath)}`, { replace: true });
        }, 1400);
      }
    };

    void run();

    return () => {
      mounted = false;
    };
  }, [applyAuthResponse, navigate, redirectPath, setOnboarding, toggleMenu]);

  return (
    <section className="relative grid min-h-screen w-full place-items-center overflow-hidden bg-slate-950 isolate">
      <img
        src="/lumos-login-bg.png"
        alt="Plano de fundo Lumos"
        className="absolute inset-0 h-full w-full object-cover opacity-20"
      />

      <div className="absolute inset-0 bg-[radial-gradient(circle_at_12%_14%,rgba(14,165,233,0.2),transparent_34%),radial-gradient(circle_at_88%_84%,rgba(20,184,166,0.16),transparent_30%),linear-gradient(135deg,rgba(2,6,23,0.88),rgba(15,23,42,0.78))]" />

      <div className="relative z-10 w-[min(92vw,440px)] rounded-3xl border border-slate-400/30 bg-slate-950/60 p-8 text-center text-slate-200 backdrop-blur-xl">
        <img
          src="/icon-192.png"
          width={72}
          height={72}
          alt="Lumos"
          className="mx-auto mb-4 drop-shadow-[0_10px_24px_rgba(14,165,233,0.3)]"
        />

        <h1 className="m-0 text-3xl font-bold text-white">
          Lumos<span className="text-sky-300">™</span>
        </h1>

        {loading && (
          <div className="mt-6 grid justify-items-center gap-2">
            <span
              aria-hidden="true"
              className="h-9 w-9 rounded-full border-[3px] border-sky-200/30 border-t-sky-400 animate-spin"
            />
            <p className="m-0 font-semibold text-slate-50">Autenticando com seguranca...</p>
            <small className="leading-6 text-slate-300">
              Estamos validando sua sessao para entrar na plataforma.
            </small>
          </div>
        )}

        {!loading && errorMessage && (
          <div className="mt-6 grid justify-items-center gap-2">
            <p className="m-0 font-semibold text-red-200">{errorMessage}</p>
          </div>
        )}
      </div>
    </section>
  );
}
