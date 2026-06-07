import { useEffect, useMemo, useState } from 'react';

import { useAppStore } from '@/store/use-app-store';
import { GlassListbox } from '@/shared/components/glass-list-box';

const cities = [
  { value: 1, label: 'Prefeitura A' },
  { value: 2, label: 'Prefeitura B' },
];

const contracts = [
  { value: 1, label: 'Contrato 001' },
  { value: 2, label: 'Contrato 002' },
];

const teamPerformance = [
  { id: 1, name: 'Equipe Alpha', hours: 400, services: 120, productivity: 88 },
  { id: 2, name: 'Equipe Beta', hours: 350, services: 95, productivity: 75 },
  { id: 3, name: 'Equipe Gama', hours: 490, services: 127, productivity: 91 },
];

const usersByTeam: Record<number, Array<{ name: string; hours: number; services: number; productivity: number }>> = {
  1: [
    { name: 'João Silva', hours: 160, services: 45, productivity: 84 },
    { name: 'Maria Souza', hours: 170, services: 52, productivity: 91 },
  ],
  2: [{ name: 'Carlos Lima', hours: 150, services: 38, productivity: 72 }],
  3: [{ name: 'Rafael Moura', hours: 180, services: 55, productivity: 92 }],
};

const riskItems = [
  { contract: 'Contrato 001', type: 'Overtime', description: 'Funcionário excedeu limite diário', severity: 'danger' },
  { contract: 'Contrato 002', type: 'Stock', description: 'Material abaixo do mínimo', severity: 'warning' },
];

export default function ExecutiveDashboard() {
  const { setPageContext } = useAppStore();

  const [city, setCity] = useState<number | null>(null);
  const [contract, setContract] = useState<number | null>(null);
  const [month, setMonth] = useState(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  });
  const [selectedTeamId, setSelectedTeamId] = useState<number | null>(null);
  const [tab, setTab] = useState<'overview' | 'teams' | 'issues'>('overview');

  useEffect(() => {
    setPageContext(['Dashboard', 'Visão Executiva'], 'Visão Executiva da operação');
  }, [setPageContext]);

  const summary = useMemo(() => ({
    avgProductivity: 87,
    totalHours: 1240,
    totalServices: 342,
    criticalInventory: 6,
    inconsistencyCount: riskItems.length,
  }), []);

  const selectedTeamUsers = selectedTeamId ? usersByTeam[selectedTeamId] ?? [] : [];

  return (
    <section className="min-h-full space-y-6 p-4 md:p-6">
      <header className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-slate-800 dark:text-slate-100">Visão Executiva da operação</h1>
      </header>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
        <GlassListbox
          value={city}
          onChange={(value) => setCity(value ? Number(value) : null)}
          options={[{ value: null, label: 'Todas as prefeituras' }, ...cities]}
          placeholder="Prefeitura"
        />
        <GlassListbox
          value={contract}
          onChange={(value) => setContract(value ? Number(value) : null)}
          options={[{ value: null, label: 'Todos os contratos' }, ...contracts]}
          placeholder="Contrato"
        />
        <input
          type="month"
          value={month}
          onChange={(event) => setMonth(event.target.value)}
          className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 outline-none focus:border-blue-400 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
        />
        <button type="button" className="rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500">
          <i className="pi pi-filter mr-2" />Aplicar Filtros
        </button>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-5">
        {[
          { title: 'Produtividade', value: `${summary.avgProductivity}%`, tone: 'text-emerald-500' },
          { title: 'Horas Trabalhadas', value: `${summary.totalHours}h` },
          { title: 'Serviços Executados', value: String(summary.totalServices) },
          { title: 'Estoque Crítico', value: String(summary.criticalInventory), tone: 'text-amber-500' },
          { title: 'Inconsistências', value: String(summary.inconsistencyCount), tone: 'text-rose-500' },
        ].map((card) => (
          <article key={card.title} className="rounded-xl border border-slate-200 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-900">
            <p className="text-xs uppercase tracking-wide text-slate-500 dark:text-zinc-400">{card.title}</p>
            <p className={`mt-2 text-3xl font-semibold text-slate-900 dark:text-slate-100 ${card.tone ?? ''}`}>{card.value}</p>
          </article>
        ))}
      </div>

      {summary.inconsistencyCount > 0 && (
        <div className="rounded-xl border-l-4 border-rose-500 bg-rose-50 p-4 font-medium text-rose-700 dark:bg-rose-500/10 dark:text-rose-300">
          <i className="pi pi-exclamation-triangle mr-2" />Foram detectadas {summary.inconsistencyCount} inconsistências críticas neste período.
        </div>
      )}

      <div className="rounded-2xl border border-slate-200 bg-white dark:border-zinc-800 dark:bg-zinc-900">
        <div className="flex gap-2 border-b border-slate-200 p-3 dark:border-zinc-800">
          {[
            { id: 'overview', label: 'Visão Geral' },
            { id: 'teams', label: 'Equipes' },
            { id: 'issues', label: 'Inconsistências' },
          ].map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => setTab(item.id as 'overview' | 'teams' | 'issues')}
              className={[
                'rounded-xl px-4 py-2 text-sm font-semibold transition',
                tab === item.id
                  ? 'bg-gradient-to-r from-blue-600 to-cyan-500 text-white'
                  : 'text-slate-600 hover:bg-slate-100 dark:text-zinc-300 dark:hover:bg-zinc-800',
              ].join(' ')}
            >
              {item.label}
            </button>
          ))}
        </div>

        {tab === 'overview' && (
          <div className="grid grid-cols-1 gap-6 p-6 md:grid-cols-2">
            <article className="rounded-xl border border-slate-200 p-4 dark:border-zinc-800">
              <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">Tendência de Produtividade</h3>
              <div className="mt-4 space-y-2">
                {[78, 82, 85, 83, 89].map((value, index) => (
                  <div key={index} className="flex items-center gap-2">
                    <span className="w-8 text-xs text-slate-500">M{index + 1}</span>
                    <div className="h-2 flex-1 rounded-full bg-slate-100 dark:bg-zinc-800">
                      <div className="h-2 rounded-full bg-gradient-to-r from-blue-600 to-cyan-500" style={{ width: `${value}%` }} />
                    </div>
                    <span className="w-10 text-right text-xs font-semibold text-slate-700 dark:text-zinc-200">{value}%</span>
                  </div>
                ))}
              </div>
            </article>

            <article className="rounded-xl border border-slate-200 p-4 dark:border-zinc-800">
              <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">Estoque por Almoxarifado</h3>
              <div className="mt-4 space-y-2">
                {[
                  { label: 'Central', value: 2 },
                  { label: 'Zona Norte', value: 3 },
                  { label: 'Zona Sul', value: 1 },
                ].map((item) => (
                  <div key={item.label} className="flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2 dark:bg-zinc-800/60">
                    <span className="text-sm text-slate-700 dark:text-zinc-200">{item.label}</span>
                    <span className="text-sm font-semibold text-amber-600 dark:text-amber-300">{item.value} críticos</span>
                  </div>
                ))}
              </div>
            </article>
          </div>
        )}

        {tab === 'teams' && (
          <div className="space-y-4 p-6">
            <article className="overflow-hidden rounded-xl border border-slate-200 dark:border-zinc-800">
              <table className="w-full text-sm">
                <thead className="bg-slate-50 text-slate-600 dark:bg-zinc-800 dark:text-zinc-300">
                  <tr>
                    <th className="px-4 py-3 text-left">Equipe</th>
                    <th className="px-4 py-3 text-left">Horas</th>
                    <th className="px-4 py-3 text-left">Serviços</th>
                    <th className="px-4 py-3 text-left">Produtividade</th>
                  </tr>
                </thead>
                <tbody>
                  {teamPerformance.map((team) => (
                    <tr
                      key={team.id}
                      onClick={() => setSelectedTeamId(team.id)}
                      className="cursor-pointer border-t border-slate-100 hover:bg-slate-50 dark:border-zinc-800 dark:hover:bg-zinc-800/70"
                    >
                      <td className="px-4 py-3 font-medium text-slate-900 dark:text-zinc-100">{team.name}</td>
                      <td className="px-4 py-3 text-slate-600 dark:text-zinc-300">{team.hours}h</td>
                      <td className="px-4 py-3 text-slate-600 dark:text-zinc-300">{team.services}</td>
                      <td className="px-4 py-3">
                        <span className={team.productivity > 85 ? 'text-emerald-500' : team.productivity > 70 ? 'text-amber-500' : 'text-rose-500'}>
                          {team.productivity}%
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </article>

            {selectedTeamId && (
              <article className="overflow-hidden rounded-xl border border-slate-200 dark:border-zinc-800">
                <header className="border-b border-slate-200 bg-slate-50 px-4 py-3 text-sm font-semibold text-slate-900 dark:border-zinc-800 dark:bg-zinc-800 dark:text-zinc-100">
                  Funcionários da Equipe
                </header>
                <table className="w-full text-sm">
                  <thead className="text-slate-600 dark:text-zinc-300">
                    <tr>
                      <th className="px-4 py-3 text-left">Funcionário</th>
                      <th className="px-4 py-3 text-left">Horas</th>
                      <th className="px-4 py-3 text-left">Serviços</th>
                      <th className="px-4 py-3 text-left">Produtividade</th>
                    </tr>
                  </thead>
                  <tbody>
                    {selectedTeamUsers.map((user) => (
                      <tr key={user.name} className="border-t border-slate-100 dark:border-zinc-800">
                        <td className="px-4 py-3 text-slate-900 dark:text-zinc-100">{user.name}</td>
                        <td className="px-4 py-3 text-slate-600 dark:text-zinc-300">{user.hours}h</td>
                        <td className="px-4 py-3 text-slate-600 dark:text-zinc-300">{user.services}</td>
                        <td className="px-4 py-3 text-slate-600 dark:text-zinc-300">{user.productivity}%</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </article>
            )}
          </div>
        )}

        {tab === 'issues' && (
          <div className="p-6">
            <article className="overflow-hidden rounded-xl border border-slate-200 dark:border-zinc-800">
              <table className="w-full text-sm">
                <thead className="bg-slate-50 text-slate-600 dark:bg-zinc-800 dark:text-zinc-300">
                  <tr>
                    <th className="px-4 py-3 text-left">Contrato</th>
                    <th className="px-4 py-3 text-left">Tipo</th>
                    <th className="px-4 py-3 text-left">Descrição</th>
                    <th className="px-4 py-3 text-left">Severidade</th>
                  </tr>
                </thead>
                <tbody>
                  {riskItems.map((item, index) => (
                    <tr key={index} className="border-t border-slate-100 dark:border-zinc-800">
                      <td className="px-4 py-3 text-slate-900 dark:text-zinc-100">{item.contract}</td>
                      <td className="px-4 py-3 text-slate-600 dark:text-zinc-300">{item.type}</td>
                      <td className="px-4 py-3 text-slate-600 dark:text-zinc-300">{item.description}</td>
                      <td className="px-4 py-3">
                        <span className={[
                          'rounded-full px-2.5 py-1 text-xs font-semibold',
                          item.severity === 'danger'
                            ? 'bg-rose-100 text-rose-700 dark:bg-rose-500/20 dark:text-rose-200'
                            : 'bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-200',
                        ].join(' ')}>
                          {item.severity === 'danger' ? 'Crítica' : 'Atenção'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </article>
          </div>
        )}
      </div>
    </section>
  );
}
