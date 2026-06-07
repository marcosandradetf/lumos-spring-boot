import { useEffect, useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

type StatusType = 'offline' | '404' | '403' | '500';

type StatusConfig = {
  code: string;
  title: string;
  message: string;
  badge: string;
  colorClass: string;
  bgClass: string;
  icon: string;
  glowClass: string;
};

const statusMap: Record<StatusType, StatusConfig> = {
  offline: {
    code: '0',
    title: 'Sem conexão com o servidor',
    message: 'Não foi possível conectar ao servidor. Verifique sua internet ou tente novamente.',
    badge: 'Sem conexão',
    colorClass: 'text-orange-500 dark:text-orange-300',
    bgClass: 'bg-orange-100 dark:bg-orange-900/30',
    icon: 'pi pi-wifi',
    glowClass: 'bg-orange-500/20',
  },
  '404': {
    code: '404',
    title: 'Página não encontrada',
    message: 'A página que você tentou acessar não existe.',
    badge: 'Recurso não encontrado',
    colorClass: 'text-blue-500 dark:text-blue-300',
    bgClass: 'bg-blue-100 dark:bg-blue-900/30',
    icon: 'pi pi-search',
    glowClass: 'bg-blue-500/20',
  },
  '403': {
    code: '403',
    title: 'Acesso negado',
    message: 'Você não possui permissão para acessar este recurso. Caso acredite que isso seja um erro, entre em contato com um administrador.',
    badge: 'Permissão insuficiente',
    colorClass: 'text-amber-500 dark:text-amber-300',
    bgClass: 'bg-amber-100 dark:bg-amber-900/30',
    icon: 'pi pi-lock',
    glowClass: 'bg-amber-500/20',
  },
  '500': {
    code: '500',
    title: 'O servidor não respondeu',
    message: 'Estamos enfrentando uma instabilidade temporária. Nossa equipe já foi notificada e está trabalhando para normalizar o sistema o mais rápido possível.',
    badge: 'Instabilidade detectada',
    colorClass: 'text-red-500 dark:text-red-300',
    bgClass: 'bg-red-100 dark:bg-red-900/30',
    icon: 'pi pi-exclamation-triangle',
    glowClass: 'bg-red-500/20',
  },
};

function isStatusType(value: string): value is StatusType {
  return value in statusMap;
}

export default function StatusPage() {
  const navigate = useNavigate();
  const { type } = useParams();

  const typeParam = type ?? '';
  const safeType: StatusType = isStatusType(typeParam) ? typeParam : '500';
  const config = useMemo(() => statusMap[safeType], [safeType]);

  useEffect(() => {
    document.title = `Lumos IP - ${config.title}`;
  }, [config.title]);

  const goBack = () => {
    if (window.history.length > 1) {
      navigate(-1);
      return;
    }
    navigate('/dashboard');
  };

  return (
    <section className="relative flex min-h-screen items-center justify-center overflow-hidden bg-slate-950 px-6 py-12">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top_left,_rgba(59,130,246,0.18),_transparent_34%),radial-gradient(circle_at_bottom_right,_rgba(20,184,166,0.15),_transparent_34%)]" />
      <div className={`pointer-events-none absolute left-1/2 top-12 h-60 w-60 -translate-x-1/2 rounded-full blur-3xl ${config.glowClass}`} />

      <div className="relative z-10 w-full max-w-lg text-center">
        <div className="mb-10 flex justify-center opacity-95">
          <img src="/lumos_logo.svg" alt="Lumos" className="h-9 w-auto" />
        </div>

        <article className="relative rounded-3xl border border-white/10 bg-slate-900/75 p-8 shadow-2xl backdrop-blur-2xl">
          <div className={`mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full ${config.bgClass}`}>
            <i className={`${config.icon} text-2xl ${config.colorClass}`} />
          </div>

          {config.code !== '0' && <p className={`mb-2 text-6xl font-bold tracking-tight ${config.colorClass}`}>{config.code}</p>}

          <h1 className="mb-3 text-2xl font-semibold text-white md:text-3xl">{config.title}</h1>
          <p className="mb-6 text-sm leading-relaxed text-slate-300 md:text-base">{config.message}</p>

          <div className={`mb-6 inline-flex items-center gap-2 rounded-full px-3 py-1.5 text-xs font-medium ${config.bgClass} ${config.colorClass}`}>
            <span className="h-2 w-2 animate-pulse rounded-full bg-current" />
            {config.badge}
          </div>

          <div className="flex flex-col justify-center gap-3 sm:flex-row">
            {safeType !== '403' && safeType !== '404' && (
              <button
                type="button"
                onClick={goBack}
                className="inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-5 py-2.5 text-sm font-semibold text-white shadow-md transition-all hover:from-blue-500 hover:to-cyan-400 active:scale-[0.98]"
              >
                <i className="pi pi-refresh text-sm" />
                Tentar novamente
              </button>
            )}

            <button
              type="button"
              onClick={goBack}
              className="inline-flex items-center justify-center gap-2 rounded-xl border border-white/15 px-5 py-2.5 text-sm font-semibold text-slate-100 transition-all hover:bg-white/10"
            >
              <i className="pi pi-arrow-left text-sm" />
              Voltar
            </button>
          </div>

          <div className="mt-6 text-xs text-slate-400">
            Código do erro: <span className="font-mono">ERR-{safeType.toUpperCase()}</span>
          </div>
        </article>

        <div className="mt-6 text-sm text-slate-400">
          Precisa de ajuda?{' '}
          <a href="https://lumosip.com.br/contato" className="font-medium text-cyan-300 hover:text-cyan-200 hover:underline">
            Fale com o suporte
          </a>
        </div>
      </div>
    </section>
  );
}
