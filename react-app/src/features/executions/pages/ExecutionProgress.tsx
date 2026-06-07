import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAppStore } from '@/store/use-app-store';
import { SkeletonTable } from '@/shared/ui/skeleton-table';
import { useExecutionsByStatus } from '@/features/executions/hooks/useExecutionsByStatus';
import type { ExecutionStatusSlug } from '@/features/executions/types';
import { EXECUTION_STATUS_MAP } from '@/features/executions/types';
import { CellEdit } from '@/shared/components/ui/cell-edit';
import { GlassListbox } from '@/shared/components/glass-list-box';
import { useMutation } from '@tanstack/react-query';
import { executionsApi } from '../api/executionsApi';
import { queryClient } from '@/app/query/queryClient';
import { useNotify } from '@/shared/hooks/use-notify';
import { useTeams } from '@/features/manage/hooks/useTeams';
import { useStockists } from '@/features/stock/hooks/use-stockists';
import { Modal, ModalBody, ModalFooter, ModalHeader } from '@/shared/ui/modal';
import { getApiErrorMessage } from '@/shared/api/http-error';

const fmtDate = (s: string | undefined) => s ? new Date(s).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit' }) : '—';

export default function ExecutionProgress() {
  const [showModal, setShowModal] = useState(false);
  const [rowPendingDelete, setRowPendingDelete] = useState<any | null>(null);
  const { status: statusSlug } = useParams<{ status: ExecutionStatusSlug }>();
  const { setPageContext } = useAppStore();
  const [editingCell, setEditingCell] = useState<string | null>(null);
  const { notify } = useNotify();
  const { data: teams = [] } = useTeams();
  const { data: stockists = [] } = useStockists();
  const navigate = useNavigate();

  const statusInfo = EXECUTION_STATUS_MAP[statusSlug as ExecutionStatusSlug];

  useEffect(() => {
    if (statusInfo) {
      setPageContext(['Ordens de Serviço', statusInfo.title], statusInfo.title);
    }
  }, [setPageContext, statusInfo]);

  const { data: executions = [], isLoading } = useExecutionsByStatus(statusInfo?.value ?? '');

  const deleteManagementMutation =
    useMutation({
      mutationFn: (reservationManagementId: number) => 
        executionsApi.deleteManagement(statusInfo?.value ?? '', reservationManagementId),
      onSuccess: () => {
        notify('Ordem de serviço excluída com sucesso.', 'success');
        queryClient.invalidateQueries({ queryKey: ['executions', statusInfo?.value] });
        setShowModal(false);
        setRowPendingDelete(null);
      },
      onError: (error: unknown) => {
        const message = getApiErrorMessage(error);
        notify(message ?? 'Não foi possível excluir a ordem de serviço.', 'error');
      },
    });

  const updateManagementMutation = useMutation({
    mutationFn: ({ reservationManagementId, userId, teamId }: { reservationManagementId: number; userId: string; teamId: number }) =>
      executionsApi.updateManagement(reservationManagementId, userId, teamId),
    onSuccess: () => {
      notify('Ordem de serviço atualizada com sucesso.', 'success');
      queryClient.invalidateQueries({ queryKey: ['executions', statusInfo?.value] });
      setEditingCell(null);
    },
    onError: (error: unknown) => {
      const message = getApiErrorMessage(error);
      notify(message ?? 'Não foi possível atualizar a ordem de serviço.', 'error');
    },
  });

  if (!statusInfo) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center p-6">
        <p className="text-slate-500 dark:text-zinc-400">Status desconhecido.</p>
      </div>
    );
  }


  const handleUpdateResponsible = (row: any, userId: string) => {
    if (!row.teamId) {
      notify('Não foi possível atualizar: equipe não definida para esta OS.', 'warn');
      return;
    }

    updateManagementMutation.mutate({
      reservationManagementId: row.reservationManagementId,
      userId,
      teamId: row.teamId,
    });
  };

  const handleUpdateTeam = (row: any, teamId: number) => {
    if (!row.userId) {
      notify('Não foi possível atualizar: estoquista não definido para esta OS.', 'warn');
      return;
    }

    updateManagementMutation.mutate({
      reservationManagementId: row.reservationManagementId,
      userId: row.userId,
      teamId,
    });
  };

  return (
    <section className="p-4 md:p-6 space-y-4">
      <div className="flex items-center gap-3">
        <h1 className="text-2xl font-semibold text-slate-800 dark:text-zinc-100">{statusInfo.title}</h1>
        {!isLoading && (
          <span className="inline-flex rounded-full bg-slate-100 dark:bg-zinc-800 px-2.5 py-1 text-xs font-semibold text-slate-600 dark:text-zinc-300">
            {executions.length}
          </span>
        )}
      </div>

      {isLoading ? (
        <SkeletonTable columns={5} />
      ) : executions.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900">
          <i className="pi pi-inbox text-4xl text-slate-300 dark:text-zinc-600 mb-3" />
          <h3 className="text-base font-semibold text-slate-600 dark:text-zinc-300">Nenhuma OS neste estado</h3>
        </div>
      ) : (
        <div className="rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead className="bg-slate-50 dark:bg-zinc-900/50 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-zinc-400">
                <tr>
                  <th className="px-4 py-3 w-[5%]">#</th>
                  <th className="px-4 py-3 w-[35%]">Ordem de Serviço</th>
                  {statusInfo.value !== 'FINISHED' && (
                    <th className="px-4 py-3 w-[20%]">Estoquista</th>
                  )}
                  <th className="px-4 py-3 w-[20%]">Equipe</th>
                  {statusSlug !== 'concluidas' ? (
                    <th className="px-4 py-3 w-[15%]">{statusSlug === 'analise-estoque' ? 'Criação' :'Disponível'} </th>
                  ) : (
                    <>
                      <th className="px-4 py-3 w-[15%]">Início</th>
                      <th className="px-4 py-3 w-[15%]">Fim</th>
                      <th className="px-4 py-3 w-[15%]">Ruas</th>
                    </>
                  )}
                  <th className="px-4 py-3 w-[5%]">+</th>
                </tr>
              </thead>
              <tbody>
                {executions.map((row, i) => (
                  <tr key={row.reservationManagementId} className="border-t border-slate-100 dark:border-zinc-800 hover:bg-slate-50 dark:hover:bg-zinc-800/50 transition-colors">
                    <td className="px-4 py-3 text-xs text-slate-400 dark:text-zinc-500">{i + 1}</td>
                    <td className="px-4 py-3 text-sm text-slate-700 dark:text-zinc-200 font-medium truncate">
                      {statusInfo.value === 'FINISHED' 
                        ? <div className='flex flex-col'>
                            <p>{row?.contractor}</p>
                            <p className='text-xs text-slate-500 dark:text-zinc-400'>Etapa {row?.step}</p>
                        </div> 
                        : row.description?.toUpperCase()
                      }
                    </td>
                    {statusInfo.value !== 'FINISHED' && (
                      <td className="px-4 py-3 text-xs text-slate-500 dark:text-zinc-400">
                        {editingCell === `userName-${row.reservationManagementId}` ? (
                          <GlassListbox
                            options={stockists.map((stockist) => ({
                              label: stockist.name,
                              value: stockist.userId,
                          }))}
                          onChange={(userId) => {
                            handleUpdateResponsible(row, userId ?? '');
                          }}
                          value={row.userId}
                          onOpenChange={(open) => {
                            if (!open) {
                              setEditingCell(null);
                            }
                          }}
                          searchable
                          disabled={updateManagementMutation.isPending}
                        />
                      ) : (
                        <CellEdit
                          children={row.userName ?? '—'}
                          onClick={() => {
                            setEditingCell(`userName-${row.reservationManagementId}`);
                          }}
                          disabled={row.installationStatus !== 'WAITING_STOCKIST'}
                        />
                      )}
                    </td>)}
                    <td className="px-4 py-3 text-xs text-slate-500 dark:text-zinc-400">
                      {editingCell === `teamName-${row.reservationManagementId}` ? (
                        <GlassListbox
                          options={teams.map((team) => ({
                            label: team.teamName,
                            value: Number(team.idTeam),
                          }))}
                          onChange={(teamId) => {
                            handleUpdateTeam(row, teamId ?? -1);
                          }}
                          value={row.teamId}
                          onOpenChange={(open) => {
                            if (!open) {
                              setEditingCell(null);
                            }
                          }}
                          searchable
                          disabled={updateManagementMutation.isPending}
                        />
                      ) : (
                        <CellEdit 
                          children={row.teamName ?? row.team_name ?? '—'}
                          onClick={() => {
                            setEditingCell(`teamName-${row.reservationManagementId}`);
                          }}
                          disabled={row.installationStatus !== 'WAITING_STOCKIST'}
                        />
                      )}
                    </td>
                    {statusSlug !== 'concluidas' ? (
                      <td className="px-4 py-3 text-xs text-slate-500 dark:text-zinc-400 whitespace-nowrap">
                        {fmtDate(row.availableAt ?? row.createdAt)}
                      </td>
                    ) : (
                      <>
                        <td className="px-4 py-3 text-xs text-slate-500 dark:text-zinc-400 whitespace-nowrap">
                          {fmtDate(row.started_at)}
                        </td>
                        <td className="px-4 py-3 text-xs text-slate-500 dark:text-zinc-400 whitespace-nowrap">
                          {fmtDate(row.finished_at)}
                        </td>
                        <td className="px-4 py-3 text-xs text-slate-500 dark:text-zinc-400">
                          {row.streets.length}
                        </td>
                      </>
                    )}
                    <td className="px-4 py-3">
                      <button
                        type="button"
                        onClick={() => {
                          if(statusSlug === 'concluidas') {
                            navigate({
                              pathname: '/relatorios/instalacoes',
                              search: `?${new URLSearchParams({ 
                                startDate: row.started_at ?? '',
                                endDate: row.finished_at ?? '',
                                installationId: row.installation_id, 
                                installationType: row.installation_type,
                                type: 'data'
                              }).toString()}`
                            });
                            return;
                          }
                          setRowPendingDelete(row);
                          setShowModal(true);
                        }}
                        className="text-red-500 hover:text-red-800 text-lg"
                      >
                        <i className={`pi ${statusSlug === 'concluidas' ? 'pi-file-pdf' : 'pi-trash'}`} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Modal open={showModal} onClose={() => setShowModal(false)} className="max-w-md">
        <ModalHeader title="Confirmar exclusão" onClose={() => setShowModal(false)} />
        <ModalBody>
          <p className="text-sm text-slate-600 dark:text-zinc-300">
            Deseja excluir a ordem de serviço
            {' '}
            <span className="font-semibold text-slate-800 dark:text-zinc-100">
              {rowPendingDelete?.description ?? 'selecionada'}
            </span>
            ?
          </p>
          <p className="mt-2 text-xs text-slate-500 dark:text-zinc-400">
            Esta ação não poderá ser desfeita.
          </p>
        </ModalBody>
        <ModalFooter>
          <button
            type="button"
            onClick={() => setShowModal(false)}
            className="rounded-xl border border-slate-200 dark:border-zinc-700 px-4 py-2 text-sm font-semibold text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors"
          >
            Cancelar
          </button>
          <button
            type="button"
            disabled={!rowPendingDelete || deleteManagementMutation.isPending}
            onClick={() => {
              if (!rowPendingDelete) return;
              deleteManagementMutation.mutate(rowPendingDelete.reservationManagementId);
            }}
            className="rounded-xl bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-500 disabled:opacity-50 transition-colors"
          >
            {deleteManagementMutation.isPending && <i className="pi pi-spin pi-spinner mr-1.5 text-xs" />}
            Excluir
          </button>
        </ModalFooter>
      </Modal>
    </section>
  );
}
