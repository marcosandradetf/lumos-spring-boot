import { useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/core/auth/useAuthStore';
import { useNotify } from '@/shared/hooks/use-notify';
import { useAppStore } from '@/store/use-app-store';
import { executionApi } from '@/features/requests/api/execution-api';
import { requestsKeys } from '@/features/requests/api/query-keys';
import type { ReserveRequest } from '@/features/requests/types/reservation';

export default function ReservationManagement() {
  const navigate = useNavigate();
  const isAdmin = useAuthStore((state) => state.user?.roles.includes('ADMIN') ?? false);
  const { notify } = useNotify();
  const setPageContext = useAppStore((state) => state.setPageContext);
  const queryClient = useQueryClient();
  const { data: reservations = [], isLoading: loadingReservations } = useQuery({
    queryKey: requestsKeys.pendingReservesForStockist(),
    queryFn: executionApi.getPendingReservesForStockist,
  });
  const cancelStep = useMutation({
    mutationFn: ({ currentIds, type }: { currentIds: number[]; type: string | null }) =>
      executionApi.cancelStep(currentIds, type),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: requestsKeys.pendingReservesForStockist() });
    },
  });
  const loading = loadingReservations || cancelStep.isPending;

  useEffect(() => {
    setPageContext(
      ['Solicitações ao Estoquista', 'Ordens de serviço de instalações'],
      'Ordens de serviço de instalações',
    );

  }, [setPageContext]);

  const getItemsQuantity = (reserve: ReserveRequest) => reserve.items.length;

  const cancelManagement = async (reserve: ReserveRequest) => {
    const type = reserve.preMeasurementId !== null ? 'PRE_MEASUREMENT' : 'DIRECT_EXECUTION';
    const id = reserve.directExecutionId ?? reserve.preMeasurementId;

    if (!id) {
      notify('Não foi possível identificar a etapa para cancelamento.', 'error');
      return;
    }

    try {
      await cancelStep.mutateAsync({
        currentIds: [id],
        type,
      });
      notify(`${reserve.description} foi excluída.`, 'success');
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Não foi possível excluir a etapa.';
      notify(message, 'error');
    }
  };

  return (
    <section className="p-4 md:p-6">
      {loading && (
        <div className="flex min-h-64 items-center justify-center rounded-2xl border border-slate-200 bg-white dark:border-zinc-800 dark:bg-zinc-900">
          <i className="pi pi-spin pi-spinner text-2xl text-blue-500" />
        </div>
      )}

      {!loading && reservations.length === 0 && (
        <div className="mx-auto mt-12 flex max-w-xl flex-col items-center rounded-2xl border border-slate-200 bg-white px-8 py-10 text-center dark:border-zinc-800 dark:bg-zinc-900">
          <i className="pi pi-inbox text-5xl text-blue-400" />
          <h2 className="mt-4 text-lg font-semibold text-slate-800 dark:text-zinc-100">
            {isAdmin ? 'Nenhuma OS cadastrada' : 'Nenhuma OS atribuída a você'}
          </h2>
          <p className="mt-2 text-sm text-slate-500 dark:text-zinc-400">
            {isAdmin
              ? 'Não existem ordens de serviço no sistema.'
              : 'Você ainda não possui ordens de serviço vinculadas.'}
          </p>
        </div>
      )}

      {!loading && reservations.length > 0 && (
        <>
          <div className="mb-5 rounded-2xl border border-slate-200 bg-white px-4 py-3 dark:border-zinc-800 dark:bg-zinc-900">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300">
                <i className={`pi ${isAdmin ? 'pi-shield' : 'pi-user'}`} />
              </div>
              <div>
                <p className="text-sm font-semibold text-slate-800 dark:text-zinc-100">
                  {isAdmin ? 'Modo Administrador' : 'Minhas solicitações'}
                </p>
                <p className="text-xs text-slate-500 dark:text-zinc-400">
                  {isAdmin
                    ? 'Você está visualizando todas as ordens de serviço do sistema.'
                    : 'Você está visualizando apenas ordens atribuídas a você.'}
                </p>
              </div>
            </div>
          </div>

          <ol className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
            {reservations.map((reserve) => (
              <li key={reserve.reservationManagementId}>
                <article className="relative h-full rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md dark:border-zinc-800 dark:bg-zinc-900">
                  <button
                    type="button"
                    onClick={(event) => {
                      event.stopPropagation();
                      void cancelManagement(reserve);
                    }}
                    className="absolute right-4 top-4 flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 transition hover:bg-red-50 hover:text-red-600 dark:text-zinc-400 dark:hover:bg-red-900/20 dark:hover:text-red-300"
                    title="Cancelar e excluir etapa"
                  >
                    <i className="pi pi-trash" />
                  </button>

                  <button
                    type="button"
                    onClick={() => navigate('/requisicoes/gerenciamento/execucao', { state: { reserve } })}
                    className="w-full text-left"
                  >
                    <div className="mb-4 flex flex-wrap items-center gap-2">
                      <span className="rounded-full bg-blue-600 px-2.5 py-1 text-xs font-semibold text-white">INSTALAÇÃO</span>
                      <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-700 dark:bg-zinc-800 dark:text-zinc-300">
                        {isAdmin ? 'VISÃO GLOBAL' : 'ATRIBUÍDO A VOCÊ'}
                      </span>
                    </div>

                    <h3 className="text-lg font-semibold leading-tight text-slate-800 dark:text-zinc-100">
                      {reserve.description}
                    </h3>

                    <p className="mt-3 text-sm text-slate-500 dark:text-zinc-400">
                      Solicitação atribuída por {reserve.assignedBy}
                    </p>

                    <p className="mt-4 text-sm font-medium text-slate-700 dark:text-zinc-300">
                      {getItemsQuantity(reserve)} itens pendentes
                    </p>
                  </button>
                </article>
              </li>
            ))}
          </ol>
        </>
      )}
    </section>
  );
}
