import { useState, useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { Modal, ModalBody, ModalFooter, ModalHeader } from '@/shared/ui/modal';
import { SkeletonTable } from '@/shared/ui/skeleton-table';
import { StockMovementStepper } from '@/features/stock/components/stock-movement-stepper';
import { stockApi } from '@/features/stock/api/stock-api';
import { stockKeys } from '@/features/stock/api/query-keys';
import type { StockMovementResponse } from '@/features/stock/types/types';

const fmt = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const fmtDate = (s: string) => new Date(s).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit' });

export default function StockMovementPending() {
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = useState<number | null>(null);

  useEffect(() => {
    setPageContext(['Estoque', 'Movimentações Pendentes'], 'Movimentações Pendentes');
  }, [setPageContext]);

  const { data: movements = [], isLoading } = useQuery({
    queryKey: stockKeys.movementsPending(),
    queryFn: stockApi.getMovements,
  });
  const approveRejectMutation = useMutation({
    mutationFn: ({ id, action }: { id: number; action: 'approve' | 'reject' }) => {
      if (action === 'approve') {
        return stockApi.approveMovement(id);
      }
      return stockApi.rejectMovement(id);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: stockKeys.movementsPending() });
    },
  });
  const actionLoading = approveRejectMutation.isPending;

  const selected = (movements as StockMovementResponse[]).find(m => m.id === selectedId);

  const handleAction = async (action: 'approve' | 'reject') => {
    if (!selectedId) return;
    try {
      await approveRejectMutation.mutateAsync({ id: selectedId, action });
      if (action === 'approve') {
        notify('Movimentação aprovada.', 'success');
      } else {
        notify('Movimentação rejeitada.', 'success');
      }
      setSelectedId(null);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Erro ao processar movimentação.';
      notify(msg, 'error');
    }
  };

  return (
    <section className="p-4 md:p-6 space-y-4">
      <StockMovementStepper />

      {isLoading ? (
        <SkeletonTable columns={7} />
      ) : (movements as StockMovementResponse[]).length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900">
          <i className="pi pi-inbox text-4xl text-slate-300 dark:text-zinc-600 mb-3" />
          <h3 className="text-base font-semibold text-slate-600 dark:text-zinc-300">Sem movimentações pendentes</h3>
          <p className="text-sm text-slate-400 dark:text-zinc-500 mt-1">Nenhuma movimentação aguardando aprovação.</p>
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
                  <th className="px-4 py-3">Qtde./Emb.</th>
                  <th className="px-4 py-3">Preço Total</th>
                  <th className="px-4 py-3">Almoxarifado</th>
                  <th className="px-4 py-3">Responsável</th>
                </tr>
              </thead>
              <tbody>
                {(movements as StockMovementResponse[]).map(m => (
                  <tr key={m.id} onClick={() => setSelectedId(m.id)}
                    className="border-t border-slate-100 dark:border-zinc-800 cursor-pointer hover:bg-slate-50 dark:hover:bg-zinc-800/50 transition-colors">
                    <td className="px-4 py-3 text-xs text-slate-500 dark:text-zinc-400 whitespace-nowrap">{fmtDate(m.dateOf)}</td>
                    <td className="px-4 py-3 text-sm text-slate-700 dark:text-zinc-200">{m.materialName}</td>
                    <td className="px-4 py-3 text-sm text-slate-700 dark:text-zinc-200 text-right">{m.inputQuantity}</td>
                    <td className="px-4 py-3 text-sm text-slate-500 dark:text-zinc-400">{m.buyUnit}</td>
                    <td className="px-4 py-3 text-sm text-slate-500 dark:text-zinc-400">{m.quantityPackage ?? '—'}</td>
                    <td className="px-4 py-3 text-sm text-slate-700 dark:text-zinc-200 whitespace-nowrap">{m.priceTotal ? fmt.format(Number(m.priceTotal)) : '—'}</td>
                    <td className="px-4 py-3 text-xs text-slate-500 dark:text-zinc-400">{m.deposit}</td>
                    <td className="px-4 py-3 text-xs text-slate-500 dark:text-zinc-400">{m.responsible}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="px-4 py-2 text-xs text-slate-400 dark:text-zinc-500">Clique em um item para aprovar ou rejeitar.</p>
        </div>
      )}

      <Modal open={selectedId !== null} onClose={() => setSelectedId(null)} confirmation>
        <ModalHeader title="Revisar Movimentação" onClose={() => setSelectedId(null)} />
        <ModalBody className="space-y-3">
          {selected && (
            <div className="space-y-2 text-sm text-slate-700 dark:text-zinc-200">
              <div className="grid grid-cols-2 gap-2">
                <span className="text-slate-500 dark:text-zinc-400">Material:</span><span className="font-medium">{selected.materialName}</span>
                <span className="text-slate-500 dark:text-zinc-400">Quantidade:</span><span>{selected.inputQuantity} {selected.buyUnit}</span>
                <span className="text-slate-500 dark:text-zinc-400">Qtde. Total:</span><span>{selected.totalQuantity} {selected.requestUnit}</span>
                <span className="text-slate-500 dark:text-zinc-400">Preço Total:</span><span>{selected.priceTotal ? fmt.format(Number(selected.priceTotal)) : '—'}</span>
                <span className="text-slate-500 dark:text-zinc-400">Almoxarifado:</span><span>{selected.deposit}</span>
                <span className="text-slate-500 dark:text-zinc-400">Responsável:</span><span>{selected.responsible}</span>
              </div>
            </div>
          )}
        </ModalBody>
        <ModalFooter>
          <button type="button" disabled={actionLoading} onClick={() => void handleAction('reject')}
            className="rounded-xl border border-slate-300 dark:border-zinc-600 px-4 py-2 text-sm font-semibold text-slate-700 dark:text-zinc-200 hover:bg-slate-50 dark:hover:bg-zinc-800 disabled:opacity-40 transition-colors">
            {actionLoading && <i className="pi pi-spin pi-spinner mr-1.5 text-xs" />}
            Rejeitar
          </button>
          <button type="button" disabled={actionLoading} onClick={() => void handleAction('approve')}
            className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 disabled:opacity-40 transition-colors">
            {actionLoading && <i className="pi pi-spin pi-spinner mr-1.5 text-xs" />}
            Aprovar
          </button>
        </ModalFooter>
      </Modal>
    </section>
  );
}
