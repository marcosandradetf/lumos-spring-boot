import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { SkeletonTable } from '@/shared/ui/skeleton-table';
import { Modal, ModalBody, ModalFooter, ModalHeader } from '@/shared/ui/modal';
import { preMeasurementsApi } from '@/features/pre-measurement/api/preMeasurementsApi';
import { preMeasurementKeys } from '@/features/pre-measurement/api/queryKeys';
import { STATUS_MAP } from '@/features/pre-measurement/types/types';
import type { ListPreMeasurementRequest } from '@/features/pre-measurement/types/types';

const fmtDate = (s: string) => new Date(s).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit' });

const TYPE_BADGE: Record<string, string> = {
  INSTALLATION: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300',
  MAINTENANCE: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300',
};

export default function PreMeasurementList() {
  const { status = 'pendente' } = useParams<{ status: string }>();
  const navigate = useNavigate();
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();
  const queryClient = useQueryClient();

  const info = STATUS_MAP[status] ?? { label: status, apiValue: status };
  const isPending = status === 'pendente';
  const isAvailable = status === 'disponivel';
  const [selectedForExecution, setSelectedForExecution] = useState<ListPreMeasurementRequest | null>(null);

  useEffect(() => {
    setPageContext(['Ordens de Serviço', info.label], info.label);
  }, [setPageContext, info.label]);

  const { data: items = [], isLoading } = useQuery({
    queryKey: preMeasurementKeys.list(info.apiValue),
    queryFn: () => preMeasurementsApi.getPreMeasurements(info.apiValue),
  });
  const markAvailableMutation = useMutation({
    mutationFn: (id: number) => preMeasurementsApi.markAsAvailable(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: preMeasurementKeys.all });
    },
  });

  const goToExecution = (preMeasurementId: number, multiTeam: boolean) => {
    navigate(`/execucao/pre-medicao/${preMeasurementId}?multiTeam=${multiTeam ? 'true' : 'false'}`);
    setSelectedForExecution(null);
  };

  return (
    <section className="p-4 md:p-6 space-y-4">
      {/* Status tabs */}
      <div className="flex items-center gap-1 rounded-xl border border-slate-200 dark:border-zinc-700 overflow-hidden w-fit text-sm font-medium">
        {Object.entries(STATUS_MAP).map(([slug, meta]) => (
          <button key={slug} type="button"
            onClick={() => navigate(`/pre-medicao/${slug}`)}
            className={`px-4 py-2 transition-colors ${status === slug ? 'bg-indigo-600 text-white' : 'text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800'}`}>
            {meta.label}
          </button>
        ))}
      </div>

      {/* List */}
      {isLoading ? (
        <SkeletonTable columns={5} />
      ) : (items as ListPreMeasurementRequest[]).length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900">
          <i className="pi pi-clipboard text-4xl text-slate-300 dark:text-zinc-600 mb-3" />
          <h3 className="text-base font-semibold text-slate-600 dark:text-zinc-300">Nenhuma pré-medição</h3>
          <p className="text-sm text-slate-400 dark:text-zinc-500 mt-1">{info.label}</p>
        </div>
      ) : (
        <div className="rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead className="bg-slate-50 dark:bg-zinc-900/50 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-zinc-400">
                <tr>
                  <th className="px-4 py-3">#</th>
                  <th className="px-4 py-3">Cidade</th>
                  <th className="px-4 py-3">Tipo</th>
                  <th className="px-4 py-3">Técnico</th>
                  <th className="px-4 py-3">Data</th>
                  <th className="px-4 py-3 text-right">Ruas / Itens</th>
                  <th className="px-4 py-3"></th>
                </tr>
              </thead>
              <tbody>
                {(items as ListPreMeasurementRequest[]).map(pm => (
                  <tr key={pm.preMeasurementId}
                    className="border-t border-slate-100 dark:border-zinc-800 hover:bg-slate-50 dark:hover:bg-zinc-800/50 transition-colors">
                    <td className="px-4 py-3 text-xs text-slate-400 dark:text-zinc-500">{pm.preMeasurementId}</td>
                    <td className="px-4 py-3 text-sm font-medium text-slate-700 dark:text-zinc-200">{pm.city}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${TYPE_BADGE[pm.preMeasurementType] ?? 'bg-slate-100 dark:bg-zinc-800 text-slate-600 dark:text-zinc-300'}`}>
                        {pm.preMeasurementType}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-600 dark:text-zinc-300">{pm.completeName}</td>
                    <td className="px-4 py-3 text-xs text-slate-500 dark:text-zinc-400 whitespace-nowrap">{fmtDate(pm.createdAt)}</td>
                    <td className="px-4 py-3 text-right text-xs text-slate-500 dark:text-zinc-400">
                      {pm.streetsSize} rua(s) / {pm.itemsSize} item(ns)
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex gap-1.5 justify-end">
                        {isAvailable ? (
                          <>
                            <button type="button"
                              onClick={() => goToExecution(pm.preMeasurementId, false)}
                              className="flex items-center gap-1.5 rounded-xl border border-amber-200 dark:border-amber-800/40 px-2.5 py-1.5 text-xs font-medium text-amber-700 dark:text-amber-300 hover:bg-amber-50 dark:hover:bg-amber-900/20 transition-colors">
                              <i className="pi pi-send text-xs" /> Delegar (1 equipe)
                            </button>
                            <button type="button"
                              onClick={() => setSelectedForExecution(pm)}
                              className="flex items-center gap-1.5 rounded-xl bg-indigo-600 px-2.5 py-1.5 text-xs font-semibold text-white hover:bg-indigo-500 transition-colors">
                              <i className="pi pi-sitemap text-xs" /> Múltiplas equipes
                            </button>
                          </>
                        ) : (
                          <button type="button"
                            onClick={() => navigate(`/pre-medicao/${status}/${pm.preMeasurementId}`)}
                            className="flex items-center gap-1.5 rounded-xl border border-slate-200 dark:border-zinc-700 px-2.5 py-1.5 text-xs font-medium text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors">
                            <i className="pi pi-eye text-xs" /> Ver
                          </button>
                        )}
                        {isPending && (
                          <button type="button"
                            disabled={markAvailableMutation.isPending}
                            onClick={() => markAvailableMutation.mutate(pm.preMeasurementId, {
                              onSuccess: () => notify('Pré-medição marcada como disponível.', 'success'),
                              onError: (error: unknown) => {
                                const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
                                notify(message ?? 'Erro ao marcar como disponível.', 'error');
                              },
                            })}
                            className="flex items-center gap-1.5 rounded-xl bg-emerald-600 px-2.5 py-1.5 text-xs font-semibold text-white hover:bg-emerald-500 disabled:opacity-50 transition-colors">
                            {markAvailableMutation.isPending
                              ? <i className="pi pi-spin pi-spinner text-xs" />
                              : <i className="pi pi-check text-xs" />}
                            Aprovar
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Modal open={selectedForExecution !== null} onClose={() => setSelectedForExecution(null)} confirmation>
        <ModalHeader title="Definir Sistemática da Execução" onClose={() => setSelectedForExecution(null)} />
        <ModalBody>
          <p className="text-sm text-slate-600 dark:text-zinc-300">
            Pré-medição de <strong>{selectedForExecution?.city}</strong>. Como deseja delegar essa execução?
          </p>
        </ModalBody>
        <ModalFooter>
          <button
            type="button"
            onClick={() => selectedForExecution && goToExecution(selectedForExecution.preMeasurementId, true)}
            className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500"
          >
            Múltiplas equipes
          </button>
          <button
            type="button"
            onClick={() => selectedForExecution && goToExecution(selectedForExecution.preMeasurementId, false)}
            className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-800"
          >
            Apenas 1 equipe
          </button>
        </ModalFooter>
      </Modal>
    </section>
  );
}
