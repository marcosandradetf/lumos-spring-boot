import { useLocation, useNavigate } from 'react-router-dom';

const STEPS = [
  { label: 'Selecionar itens', path: '/estoque/movimentar-estoque' },
  { label: 'Pendente de Aprovação', path: '/estoque/movimentar-estoque-pendente' },
  { label: 'Aprovado', path: '/estoque/movimentar-estoque-aprovado' },
] as const;

export function StockMovementStepper() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const current = STEPS.findIndex(s => s.path === pathname);

  return (
    <div className="flex items-center justify-center flex-wrap gap-1 py-3 px-4 mb-4 rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900">
      {STEPS.map((step, i) => (
        <div key={step.path} className="flex items-center">
          <button
            type="button"
            onClick={() => navigate(step.path)}
            className={[
              'flex items-center gap-2 px-3 py-1.5 rounded-xl text-sm font-medium transition-colors',
              i === current
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-slate-500 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800',
            ].join(' ')}
          >
            <span className={[
              'flex h-5 w-5 items-center justify-center rounded-full text-[11px] font-bold flex-shrink-0',
              i === current
                ? 'bg-white/20'
                : i < current
                  ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300'
                  : 'bg-slate-100 dark:bg-zinc-800 text-slate-500 dark:text-zinc-400',
            ].join(' ')}>
              {i < current ? <i className="pi pi-check text-[9px]" /> : i + 1}
            </span>
            {step.label}
          </button>
          {i < STEPS.length - 1 && (
            <i className="pi pi-chevron-right text-[10px] text-slate-300 dark:text-zinc-600 mx-1" />
          )}
        </div>
      ))}
    </div>
  );
}
