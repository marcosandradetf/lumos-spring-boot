import { useEffect, useMemo, useState } from 'react';

import { useAppStore } from '@/store/use-app-store';

const teamPerformanceSeed = [
  { name: 'Equipe Alpha', meta: 300, real: 330 },
  { name: 'Equipe Beta', meta: 280, real: 250 },
  { name: 'Equipe Gama', meta: 250, real: 220 },
  { name: 'Equipe X', meta: 250, real: 290 },
  { name: 'Equipe Y', meta: 290, real: 290 },
];

const collaboratorRanking = [
  { position: 1, name: 'João Silva', services: 140 },
  { position: 2, name: 'Maria Souza', services: 120 },
  { position: 3, name: 'Carlos Lima', services: 95 },
];

const collaborators = [
  { name: 'João Silva', team: 'Equipe Alpha', workedHours: 230, overtimeHours: 10, servicesExecuted: 140, ledInstallations: 85, productivity: 92 },
  { name: 'Maria Souza', team: 'Equipe Alpha', workedHours: 210, overtimeHours: 0, servicesExecuted: 120, ledInstallations: 70, productivity: 85 },
  { name: 'Carlos Lima', team: 'Equipe Beta', workedHours: 240, overtimeHours: 20, servicesExecuted: 95, ledInstallations: 55, productivity: 72 },
];

function getMonthRange() {
  const today = new Date();
  const start = new Date(today.getFullYear(), today.getMonth(), 1);
  const end = new Date(today.getFullYear(), today.getMonth() + 1, 0);
  return { start, end };
}

function formatDateInput(value: Date) {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const day = String(value.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export default function TeamProductivityDashboard() {
  const { setPageContext } = useAppStore();
  const monthRange = useMemo(() => getMonthRange(), []);

  const [startDate, setStartDate] = useState(formatDateInput(monthRange.start));
  const [endDate, setEndDate] = useState(formatDateInput(monthRange.end));

  useEffect(() => {
    setPageContext(['Dashboard', 'Produtividade da Equipe'], 'Performance Operacional');
  }, [setPageContext]);

  const teamPerformance = useMemo(() => {
    const best = [...teamPerformanceSeed].sort((a, b) => b.real - a.real)[0]?.name;
    return teamPerformanceSeed.map((team) => {
      const percentage = (team.real / team.meta) * 100;
      return {
        ...team,
        animatedWidth: Math.min(percentage, 120),
        achieved: team.real >= team.meta,
        superAchieved: team.real >= team.meta * 1.1,
        isBest: team.name === best,
      };
    });
  }, []);

  const kpis = {
    totalTeams: 3,
    totalCollaborators: 8,
    totalServicesExecuted: 780,
    totalLedInstallations: 420,
    totalOvertimeHours: 34,
    avgProductivity: 86,
  };

  return (
    <section className="mx-auto max-w-7xl space-y-10 p-4 md:p-8">
      <header className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight text-slate-900 dark:text-slate-100">Performance Operacional</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">Visão consolidada por período selecionado.</p>
        </div>

        <div className="flex items-center gap-3 rounded-xl border border-slate-200 bg-slate-100 px-5 py-3 dark:border-slate-700 dark:bg-slate-800">
          <i className="pi pi-calendar text-slate-500 dark:text-slate-400" />
          <div className="flex items-center gap-2 text-sm">
            <input
              type="date"
              value={startDate}
              onChange={(event) => setStartDate(event.target.value)}
              className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-slate-800 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
            />
            <span className="text-slate-500">até</span>
            <input
              type="date"
              value={endDate}
              onChange={(event) => setEndDate(event.target.value)}
              className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-slate-800 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
            />
          </div>
        </div>
      </header>

      <section className="grid grid-cols-1 gap-6 md:grid-cols-3 xl:grid-cols-6">
        {[
          { title: 'Equipes', value: kpis.totalTeams, icon: 'pi-users' },
          { title: 'Colaboradores', value: kpis.totalCollaborators, icon: 'pi-id-card' },
          { title: 'Serviços Executados', value: kpis.totalServicesExecuted, icon: 'pi-chart-line', tone: 'border-blue-200 text-blue-700 dark:border-blue-800 dark:text-blue-400' },
          { title: 'Instalações LED', value: kpis.totalLedInstallations, icon: 'pi-bolt' },
          { title: 'Hora Extra Total', value: `${kpis.totalOvertimeHours}h`, icon: 'pi-clock', tone: 'border-amber-200 text-amber-700 dark:border-amber-800 dark:text-amber-400' },
          { title: 'Produtividade Média', value: `${kpis.avgProductivity}%`, icon: 'pi-percentage' },
        ].map((item) => (
          <article
            key={item.title}
            className={`rounded-xl border border-slate-200 bg-white p-4 transition hover:shadow-lg dark:border-slate-700 dark:bg-slate-900 ${item.tone ?? ''}`}
          >
            <div className="flex items-start justify-between">
              <div>
                <p className="text-sm text-slate-500 dark:text-slate-400">{item.title}</p>
                <p className="mt-3 text-4xl font-semibold text-slate-900 dark:text-slate-100">{item.value}</p>
              </div>
              <i className={`pi ${item.icon} text-2xl text-slate-300 dark:text-slate-600`} />
            </div>
          </article>
        ))}
      </section>

      <section className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-700 dark:bg-slate-900">
        <h2 className="mb-6 text-lg font-semibold text-slate-900 dark:text-slate-100">Performance por Equipe</h2>

        <div className="space-y-8">
          {teamPerformance.map((team) => (
            <div key={team.name}>
              <div className="mb-2 flex flex-wrap items-center justify-between gap-2 font-medium text-slate-800 dark:text-slate-100">
                <span>{team.name}</span>
                <div className="flex flex-wrap gap-2 text-xs">
                  {team.isBest && (
                    <span className="inline-flex items-center gap-1 text-indigo-600 dark:text-indigo-400"><i className="pi pi-trophy" />Melhor desempenho</span>
                  )}
                  {team.superAchieved && (
                    <span className="inline-flex items-center gap-1 text-purple-600 dark:text-purple-400"><i className="pi pi-star-fill" />Superou meta</span>
                  )}
                  {team.achieved && !team.superAchieved && (
                    <span className="inline-flex items-center gap-1 text-blue-600 dark:text-blue-400"><i className="pi pi-check-circle" />Meta atingida</span>
                  )}
                </div>
              </div>

              <div className="mb-2 flex justify-between text-xs text-slate-500 dark:text-slate-400">
                <span>Meta: {team.meta}</span>
                <span>Realizado: {team.real}</span>
                <span>{Math.round((team.real / team.meta) * 100)}%</span>
              </div>

              <div className="h-3 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-700">
                <div
                  className={[
                    'h-3 rounded-full transition-all duration-1000 ease-out',
                    team.superAchieved
                      ? 'bg-purple-600 dark:bg-purple-500'
                      : team.achieved
                        ? 'bg-blue-600 dark:bg-blue-500'
                        : 'bg-slate-500 dark:bg-slate-600',
                  ].join(' ')}
                  style={{ width: `${team.animatedWidth}%` }}
                />
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-700 dark:bg-slate-900">
        <h2 className="mb-4 text-lg font-semibold text-slate-900 dark:text-slate-100">Ranking por Serviços Executados</h2>
        <div className="space-y-2">
          {collaboratorRanking.map((collaborator) => (
            <div key={collaborator.position} className="flex items-center justify-between rounded-lg px-2 py-3 transition hover:bg-slate-100 dark:hover:bg-slate-800">
              <div className="flex items-center gap-3">
                <span className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-200 text-sm font-semibold text-slate-800 dark:bg-slate-700 dark:text-slate-100">
                  {collaborator.position}
                </span>
                <span className="font-medium text-slate-800 dark:text-slate-100">{collaborator.name}</span>
              </div>
              <span className="font-semibold text-blue-700 dark:text-blue-400">{collaborator.services} serviços</span>
            </div>
          ))}
        </div>
      </section>

      <section className="overflow-hidden rounded-xl border border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-900">
        <header className="border-b border-slate-200 px-6 py-4 dark:border-slate-700">
          <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">Desempenho Individual</h2>
        </header>

        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-slate-600 dark:bg-slate-800 dark:text-slate-300">
            <tr>
              <th className="px-4 py-3 text-left">Colaborador</th>
              <th className="px-4 py-3 text-left">Equipe</th>
              <th className="px-4 py-3 text-left">Horas</th>
              <th className="px-4 py-3 text-left">Hora Extra</th>
              <th className="px-4 py-3 text-left">Serviços</th>
              <th className="px-4 py-3 text-left">LEDs</th>
              <th className="px-4 py-3 text-left">Produtividade</th>
            </tr>
          </thead>
          <tbody>
            {collaborators.map((user) => (
              <tr key={user.name} className="border-t border-slate-100 dark:border-slate-800">
                <td className="px-4 py-3 font-medium text-slate-900 dark:text-slate-100">{user.name}</td>
                <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{user.team}</td>
                <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{user.workedHours}h</td>
                <td className="px-4 py-3 text-purple-700 dark:text-purple-300">{user.overtimeHours}h</td>
                <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{user.servicesExecuted}</td>
                <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{user.ledInstallations}</td>
                <td className="px-4 py-3">
                  <span className={user.productivity > 85 ? 'text-blue-700 dark:text-blue-400' : user.productivity > 70 ? 'text-amber-700 dark:text-amber-400' : 'text-red-700 dark:text-red-400'}>
                    {user.productivity}%
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </section>
  );
}
