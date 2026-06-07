import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAppStore } from '@/store/use-app-store';
import { dashboardApi, type DashboardMetric } from '../api/dashboardApi';

interface DashboardProps {
  contractMode?: boolean;
}

const QUICK_ACTIONS = [
  {
    label: 'Movimentar Estoque',
    icon: 'pi-arrow-right-arrow-left',
    path: '/estoque/movimentar-estoque',
    color: 'text-blue-600 bg-blue-50 dark:bg-blue-900/20 dark:text-blue-400',
  },
  {
    label: 'Nova Ordem de Serviço',
    icon: 'pi-plus-circle',
    path: '/contratos/listar',
    query: '?for=execution',
    color: 'text-emerald-600 bg-emerald-50 dark:bg-emerald-900/20 dark:text-emerald-400',
  },
  {
    label: 'Mapa de Execuções',
    icon: 'pi-map',
    path: '/dashboard/mapa-execucoes',
    color: 'text-amber-600 bg-amber-50 dark:bg-amber-900/20 dark:text-amber-400',
  },
  {
    label: 'Relatório de Manutenções',
    icon: 'pi-file-pdf',
    path: '/relatorios/manutencoes',
    color: 'text-red-500 bg-red-50 dark:bg-red-900/20 dark:text-red-400',
  },
  {
    label: 'Catálogo de Materiais',
    icon: 'pi-table',
    path: '/estoque/catalogo-materiais',
    color: 'text-indigo-600 bg-indigo-50 dark:bg-indigo-900/20 dark:text-indigo-400',
  },
  {
    label: 'Listar Contratos',
    icon: 'pi-folder-open',
    path: '/contratos/listar',
    color: 'text-purple-600 bg-purple-50 dark:bg-purple-900/20 dark:text-purple-400',
  },
];

const FALLBACK_METRICS: DashboardMetric[] = [
  { label: 'Pré-medições', value: 0, description: 'Em aberto', routerLink: '/pre-medicao/pendente', queryParams: null },
  { label: 'Ordens de Serviço', value: 0, description: 'Em aberto', routerLink: '/requisicoes/instalacoes/gerenciamento-estoque', queryParams: null },
  { label: 'Instalações', value: 0, description: 'Em curso', routerLink: '/execucoes/em-execucao', queryParams: null },
  {
    label: 'Contratos com baixo saldo',
    value: 0,
    description: 'Precisam de atenção',
    classification: 'Ação imediata',
    routerLink: '/contratos/listar',
    queryParams: { for: 'view' },
  },
  {
    label: 'Materiais com baixo estoque',
    value: 0,
    description: 'Precisam de atenção',
    classification: 'Atenção',
    routerLink: '/estoque/catalogo-materiais',
    queryParams: null,
  },
  {
    label: 'Relatórios gerados',
    value: 0,
    description: 'Últimos 30 dias',
    classification: 'Últimos 30 dias',
    routerLink: '/relatorios/manutencoes',
    queryParams: null,
  },
];

const METRIC_BORDER_CLASS: Record<string, string> = {
  'Ação imediata':
    'border-fuchsia-300 dark:border-fuchsia-600/50',
  Crítico:
    'border-rose-300 dark:border-rose-600/50',
  Atenção:
    'border-amber-300 dark:border-amber-600/50',
  Monitorar:
    'border-cyan-300 dark:border-cyan-600/50',
  'Últimos 30 dias':
    'border-zinc-300 dark:border-zinc-600/50',
};

const METRIC_BADGE: Record<string, string> = {
  'Ação imediata': 'bg-fuchsia-600 text-white dark:bg-fuchsia-500',
  Crítico: 'bg-rose-600 text-white dark:bg-rose-500',
  Atenção: 'bg-amber-500 text-black dark:bg-amber-400 dark:text-black',
  Monitorar: 'bg-cyan-600 text-white dark:bg-cyan-500',
  'Últimos 30 dias': 'bg-zinc-600 text-white dark:bg-zinc-500',
};

export default function Dashboard({ contractMode }: DashboardProps) {
  const navigate = useNavigate();
  const { setPageContext } = useAppStore();

  useEffect(() => {
    setPageContext(contractMode ? ['Contratos', 'Dashboard'] : ['Dashboard'], contractMode ? 'Dashboard de Contratos' : '');
  }, [setPageContext, contractMode]);

  const { data: metricsRaw } = useQuery({
    queryKey: ['dashboard', 'metrics'],
    queryFn: dashboardApi.getMetrics,
  });

  const metrics: DashboardMetric[] = (metricsRaw ?? FALLBACK_METRICS) as DashboardMetric[];

  const goTo = (metric: DashboardMetric) => {
    if (!metric.routerLink) return;
    const qs = metric.queryParams ? '?' + new URLSearchParams(metric.queryParams as Record<string, string>).toString() : '';
    navigate(metric.routerLink + qs);
  };

  return (
    <section className="p-4 md:p-6 space-y-6">
      {/* Quick actions */}
      <div>
        <h1 className="text-xl font-semibold text-slate-700 dark:text-zinc-200 mb-3">Ações rápidas</h1>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          {QUICK_ACTIONS.map((a, i) => (
            <button
              key={i}
              type="button"
              onClick={() => navigate(a.path + (a.query ?? ''))}
              className="flex items-center gap-3 rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-4 text-left hover:shadow-md hover:border-indigo-200 dark:hover:border-indigo-800 transition-all group"
            >
              <div className={`flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-xl ${a.color}`}>
                <i className={`pi ${a.icon} text-sm`} />
              </div>
              <span className="text-sm font-medium text-slate-700 dark:text-zinc-200 leading-snug group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
                {a.label}
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* Greeting */}
      <div>
        <h2 className="text-xl font-semibold text-slate-800 dark:text-zinc-100">
          {contractMode ? 'Dashboard de Contratos' : 'Visão geral'}
        </h2>
      </div>

      {/* Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
        {metrics.map((m, i) => (
          <div
            key={i}
            onClick={() => goTo(m)}
            className={[
              'rounded-2xl border bg-white dark:bg-zinc-900 p-4 space-y-1 transition-all',
              m.value > 0 && m.classification
                ? METRIC_BORDER_CLASS[m.classification] ?? 'border-slate-200 dark:border-zinc-800'
                : 'border-slate-200 dark:border-zinc-800',
              m.routerLink ? 'cursor-pointer hover:shadow-md hover:border-indigo-200 dark:hover:border-indigo-800' : '',
            ].join(' ')}
          >
            <div className="flex items-start justify-between gap-2">
              <p className="text-xs font-medium text-slate-500 dark:text-zinc-400">{m.label}</p>
              {m.value > 0 && m.classification && (
                <span
                  className={[
                    'rounded-full px-2 py-0.5 text-[10px] font-semibold',
                    METRIC_BADGE[m.classification] ?? 'bg-zinc-600 text-white',
                  ].join(' ')}
                >
                  {m.classification}
                </span>
              )}
            </div>

            <div className="flex items-center justify-between">
              <p className="text-3xl font-bold text-slate-800 dark:text-zinc-100 leading-none">
                {m.value}
              </p>
              {m.routerLink && <i className="pi pi-chevron-right text-slate-400" />}
            </div>
            <p className="text-xs text-slate-400 dark:text-zinc-500">{m.description}</p>
          </div>
        ))}
      </div>
    </section>
  );
}
