import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import { useAppStore } from '@/store/use-app-store';
import { LoadingOverlay } from '@/shared/ui/loading-overlay';
import { preMeasurementsApi } from '@/features/pre-measurement/api/preMeasurementsApi';
import { preMeasurementKeys } from '@/features/pre-measurement/api/queryKeys';
import type { CheckBalanceRequest } from '@/features/pre-measurement/types/types';

const ITEM_STATUS: Record<string, string> = {
  OK: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300',
  INVALID: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300',
  WARNING: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300',
};

export default function PreMeasurementView() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { setPageContext } = useAppStore();

  useEffect(() => {
    setPageContext(['Pré-medições', 'Visualizar'], 'Visualizar Pré-medição');
  }, [setPageContext]);

  const { data: pm, isLoading: loadingPm } = useQuery({
    queryKey: preMeasurementKeys.detail(id),
    queryFn: () => preMeasurementsApi.getPreMeasurement(id),
    enabled: !!id,
  });
  const { data: balance = [], isLoading: loadingBalance } = useQuery({
    queryKey: preMeasurementKeys.balance(id, pm?.preMeasurementId),
    queryFn: () => preMeasurementsApi.checkBalance(pm?.preMeasurementId as number),
    enabled: !!id && !!pm?.preMeasurementId,
  });

  const loading = loadingPm || loadingBalance;

  return (
    <section className="p-4 md:p-6 space-y-5 relative">
      <LoadingOverlay loading={loading} />

      <div className="flex items-center gap-3">
        <button type="button" onClick={() => navigate(-1)}
          className="flex items-center justify-center w-8 h-8 rounded-full bg-slate-100 dark:bg-zinc-800 text-slate-600 dark:text-zinc-300 hover:bg-slate-200 dark:hover:bg-zinc-700 transition-colors">
          <i className="pi pi-arrow-left text-xs" />
        </button>
        <h1 className="text-xl font-semibold text-slate-800 dark:text-zinc-100">
          {pm ? `Pré-medição #${pm.preMeasurementId} — ${pm.city}` : 'Carregando...'}
        </h1>
      </div>

      {pm && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          {[
            { label: 'Tipo', value: pm.preMeasurementType },
            { label: 'Técnico', value: pm.completeName },
            { label: 'Status', value: pm.status },
            { label: 'Etapa', value: String(pm.step) },
          ].map(item => (
            <div key={item.label} className="rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-3">
              <p className="text-xs text-slate-500 dark:text-zinc-400">{item.label}</p>
              <p className="text-sm font-semibold text-slate-800 dark:text-zinc-100 mt-0.5">{item.value}</p>
            </div>
          ))}
        </div>
      )}

      {/* Balance check */}
      {(balance as CheckBalanceRequest[]).length > 0 && (
        <div>
          <h2 className="text-sm font-semibold text-slate-700 dark:text-zinc-200 mb-2">Balanço dos Itens</h2>
          <div className="rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead className="bg-slate-50 dark:bg-zinc-900/50 text-xs font-semibold uppercase text-slate-500 dark:text-zinc-400">
                  <tr>
                    <th className="px-4 py-3 text-left min-w-[180px]">Descrição</th>
                    <th className="px-4 py-3 text-right">Medido</th>
                    <th className="px-4 py-3 text-right">Saldo</th>
                    <th className="px-4 py-3 text-right">Contratado</th>
                    <th className="px-4 py-3 text-right">Executado</th>
                    <th className="px-4 py-3 text-right">Saldo atual</th>
                  </tr>
                </thead>
                <tbody>
                  {(balance as CheckBalanceRequest[]).map((b, i) => (
                    <tr key={i} className="border-t border-slate-100 dark:border-zinc-800">
                      <td className="px-4 py-3 text-slate-700 dark:text-zinc-200 font-medium">{b.description}</td>
                      <td className="px-4 py-3 text-right text-slate-600 dark:text-zinc-300">{b.totalMeasured}</td>
                      <td className="px-4 py-3 text-right text-slate-600 dark:text-zinc-300">{b.totalBalance}</td>
                      <td className="px-4 py-3 text-right text-slate-600 dark:text-zinc-300">{b.totalContractedQuantity}</td>
                      <td className="px-4 py-3 text-right text-slate-600 dark:text-zinc-300">{b.totalQuantityExecuted}</td>
                      <td className="px-4 py-3 text-right font-semibold">
                        <span className={Number(b.totalCurrentBalance) < 0
                          ? 'text-red-600 dark:text-red-400'
                          : 'text-emerald-600 dark:text-emerald-400'}>
                          {b.totalCurrentBalance}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Streets */}
      {pm && pm.streets.length > 0 && (
        <div>
          <h2 className="text-sm font-semibold text-slate-700 dark:text-zinc-200 mb-2">
            Logradouros ({pm.streets.length})
          </h2>
          <div className="space-y-2">
            {pm.streets.map(street => (
              <div key={street.preMeasurementStreetId}
                className="rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 overflow-hidden">
                <div className="flex items-center justify-between px-4 py-2.5 bg-slate-50 dark:bg-zinc-900/50 border-b border-slate-100 dark:border-zinc-800">
                  <p className="text-sm font-medium text-slate-700 dark:text-zinc-200">{street.address || `Logradouro ${street.preMeasurementStreetId}`}</p>
                  <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-semibold ${ITEM_STATUS[street.status] ?? 'bg-slate-100 dark:bg-zinc-800 text-slate-600 dark:text-zinc-300'}`}>
                    {street.status}
                  </span>
                </div>
                <div className="divide-y divide-slate-50 dark:divide-zinc-800/50">
                  {street.items.map(item => (
                    <div key={item.preMeasurementStreetItemId} className="flex items-center justify-between px-4 py-2 text-xs">
                      <span className="text-slate-600 dark:text-zinc-300 truncate max-w-[60%]">{item.contractReferenceItemName}</span>
                      <div className="flex items-center gap-3 text-slate-500 dark:text-zinc-400">
                        <span>Medido: <strong className="text-slate-700 dark:text-zinc-200">{item.measuredQuantity}</strong></span>
                        <span>Saldo: <strong className={item.differenceQuantity < 0 ? 'text-red-500' : 'text-emerald-500'}>{item.differenceQuantity}</strong></span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}
