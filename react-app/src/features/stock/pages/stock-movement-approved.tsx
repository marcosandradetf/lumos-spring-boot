import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAppStore } from '@/store/use-app-store';
import { SkeletonTable } from '@/shared/ui/skeleton-table';
import { StockMovementStepper } from '@/features/stock/components/stock-movement-stepper';
import { stockApi } from '@/features/stock/api/stock-api';
import { stockKeys } from '@/features/stock/api/query-keys';
import type { StockMovementResponse } from '@/features/stock/types/types';

const fmt = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const fmtDate = (s: string) => new Date(s).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit' });

export default function StockMovementApproved() {
  const { setPageContext } = useAppStore();

  useEffect(() => {
    setPageContext(['Estoque', 'Movimentações Aprovadas'], 'Movimentações Aprovadas');
  }, [setPageContext]);

  const { data: movements = [], isLoading } = useQuery({
    queryKey: stockKeys.movementsApproved(),
    queryFn: stockApi.getMovementsApproved,
  });

  return (
    <section className="p-4 md:p-6 space-y-4">
      <StockMovementStepper />

      {isLoading ? (
        <SkeletonTable columns={8} />
      ) : (movements as StockMovementResponse[]).length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900">
          <i className="pi pi-check-circle text-4xl text-slate-300 dark:text-zinc-600 mb-3" />
          <h3 className="text-base font-semibold text-slate-600 dark:text-zinc-300">Nenhuma movimentação aprovada</h3>
        </div>
      ) : (
        <div className="rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead className="bg-slate-50 dark:bg-zinc-900/50 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-zinc-400">
                <tr>
                  <th className="px-4 py-3">Data/Hora</th>
                  <th className="px-4 py-3 min-w-[200px]">Material</th>
                  <th className="px-4 py-3">Qtde.</th>
                  <th className="px-4 py-3">UN. Compra</th>
                  <th className="px-4 py-3">Qtde. Total</th>
                  <th className="px-4 py-3">UN. Req.</th>
                  <th className="px-4 py-3">Preço Total</th>
                  <th className="px-4 py-3">Preço/Item</th>
                  <th className="px-4 py-3">Almoxarifado</th>
                  <th className="px-4 py-3">Responsável</th>
                </tr>
              </thead>
              <tbody>
                {(movements as StockMovementResponse[]).map(m => (
                  <tr key={m.id} className="border-t border-slate-100 dark:border-zinc-800 transition-colors">
                    <td className="px-4 py-3 text-xs text-slate-500 dark:text-zinc-400 whitespace-nowrap">{fmtDate(m.dateOf)}</td>
                    <td className="px-4 py-3 text-sm text-slate-700 dark:text-zinc-200">{m.materialName}</td>
                    <td className="px-4 py-3 text-sm text-right">{m.inputQuantity}</td>
                    <td className="px-4 py-3 text-sm text-slate-500 dark:text-zinc-400">{m.buyUnit}</td>
                    <td className="px-4 py-3 text-sm text-right">{m.totalQuantity}</td>
                    <td className="px-4 py-3 text-sm text-slate-500 dark:text-zinc-400">{m.requestUnit}</td>
                    <td className="px-4 py-3 text-sm whitespace-nowrap">{m.priceTotal ? fmt.format(Number(m.priceTotal)) : '—'}</td>
                    <td className="px-4 py-3 text-sm whitespace-nowrap">{m.pricePerItem ? fmt.format(Number(m.pricePerItem)) : '—'}</td>
                    <td className="px-4 py-3 text-xs text-slate-500 dark:text-zinc-400">{m.deposit}</td>
                    <td className="px-4 py-3 text-xs text-slate-500 dark:text-zinc-400">{m.responsible}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </section>
  );
}
