import { useState, useEffect, useMemo } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/core/auth/useAuthStore';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { Modal, ModalHeader, ModalBody, ModalFooter } from '@/shared/ui/modal';
import { GlassListbox } from '@/shared/components/glass-list-box';
import { EmbeddedDocPanel } from '@/shared/components/embedded-doc-panel';
import { useUsers } from '@/features/manage/hooks/useUsers';

import { stockKeys } from '@/features/stock/api/query-keys';
import { useStockists } from '@/features/stock/hooks/use-stockists';
import { useDeposits } from '@/features/stock/hooks/use-deposits';
import type { Deposit } from '@/features/stock/types/types';
import { stockistApi } from '../api/stockist-api';
import { Table, TableRow, TableHead, TableHeader, TableCell, TableBody } from '@/shared/components/ui/table';
import {showPhoneFormatted} from '@/shared/utils/formatters';

export default function Stockists() {
  const { setPageContext } = useAppStore();
  const { user } = useAuthStore();
  const { notify } = useNotify();
  const queryClient = useQueryClient();

  const [modalOpen, setModalOpen] = useState(false);
  const [deleteId, setDeleteId] = useState<number | null>(null);
  const [docOpen, setDocOpen] = useState(false);
  const [form, setForm] = useState({ depositId: '', userId: '' });

  useEffect(() => {
    setPageContext(['Configurações', 'Estoquistas'], 'Estoquistas');
  }, [setPageContext]);

  const { data: stockists = [], isLoading } = useStockists();
  const { data: depositsRaw = [] } = useDeposits();
  const { data: users = [] } = useUsers();

  const isAdmin = user?.roles.includes('ADMIN') ?? false;
  const saveMutation = useMutation({
    mutationFn: ({ depositId, userId }: { depositId: string; userId: string }) => stockistApi.insertStockist({
      depositIdDeposit: Number(depositId),
      userIdUser: userId,
    }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: stockKeys.stockists() });
    },
  });
  const deleteMutation = useMutation({
    mutationFn: (id: number) => stockistApi.deleteStockist(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: stockKeys.stockists() });
    },
  });

  const deposits = useMemo(
    () => (depositsRaw as Deposit[]).filter((deposit) => !deposit.isTruck),
    [depositsRaw],
  );
  const hasNoDeposits = deposits.length === 0;

  return (
    <section className="p-4 md:p-6 space-y-4">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-800 dark:text-zinc-100">Estoquistas</h1>
          <p className="text-sm text-slate-500 dark:text-zinc-400">
            Cadastre e gerencie os responsáveis pelos almoxarifados do sistema.
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => setDocOpen(true)}
            className="flex items-center gap-2 rounded-xl border border-slate-200 dark:border-zinc-700 px-4 py-2 text-sm font-semibold text-slate-700 dark:text-zinc-200 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors"
          >
            <i className="pi pi-book text-sm" /> Como cadastrar
          </button>

          {isAdmin && (
            <button
              type="button"
              onClick={() => {
                setForm({ depositId: '', userId: '' });
                setModalOpen(true);
              }}
              disabled={hasNoDeposits}
              className="flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
            >
              <i className="pi pi-plus text-sm" /> Novo Estoquista
            </button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <div className="rounded-2xl border border-neutral-200 bg-white p-4 shadow-sm dark:border-neutral-800 dark:bg-neutral-900">
          <p className="text-sm text-slate-500 dark:text-zinc-400">Estoquistas vinculados</p>
          <p className="mt-2 text-3xl font-semibold text-slate-900 dark:text-zinc-100">{stockists.length}</p>
        </div>

        <div className="rounded-2xl border border-neutral-200 bg-white p-4 shadow-sm dark:border-neutral-800 dark:bg-neutral-900">
          <p className="text-sm text-slate-500 dark:text-zinc-400">Almoxarifados disponíveis</p>
          <p className="mt-2 text-3xl font-semibold text-slate-900 dark:text-zinc-100">{deposits.length}</p>
        </div>

        <div className="rounded-2xl border border-neutral-200 bg-white p-4 shadow-sm dark:border-neutral-800 dark:bg-neutral-900">
          <p className="text-sm text-slate-500 dark:text-zinc-400">Usuários ativos</p>
          <p className="mt-2 text-3xl font-semibold text-slate-900 dark:text-zinc-100">{users.length}</p>
        </div>
      </div>

      {hasNoDeposits && !isLoading ? (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-5 dark:border-amber-900 dark:bg-amber-950/30">
          <h3 className="text-base font-semibold text-amber-900 dark:text-amber-100">
            Antes de vincular estoquistas...
          </h3>
          <p className="mt-1 text-sm text-amber-800 dark:text-amber-200">
            Você precisa cadastrar almoxarifados para liberar este fluxo.
          </p>
          <button
            type="button"
            onClick={() => window.location.assign('/estoque/almoxarifados')}
            className="mt-4 inline-flex items-center gap-2 rounded-xl bg-amber-600 px-4 py-2 text-sm font-semibold text-white hover:bg-amber-500 transition-colors"
          >
            <i className="pi pi-home text-sm" />
            Cadastrar Almoxarifados
          </button>
        </div>
      ) : (

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Usuário</TableHead>
              <TableHead>Almoxarifado</TableHead>
              <TableHead>Contato</TableHead>
              <TableHead>Ações</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {stockists.map((stockist) => {
              const deposit = deposits.find((d) => d.idDeposit === stockist.depositId);

              return (
                <TableRow key={stockist.stockistId ?? stockist.userId}>
                  <TableCell>
                    <p className="font-medium">{stockist.name}</p>
                  </TableCell>
                  <TableCell>
                    <div>
                      <p className="font-semibold">{stockist.depositName}</p>
                      <p className="text-xs text-slate-500 dark:text-zinc-400">{deposit?.depositRegion ?? ''}</p>
                    </div>
                  </TableCell>
                  <TableCell>
                    <p 
                      className="text-sm text-slate-600 dark:text-zinc-300" >
                      {deposit?.depositPhone && deposit.depositPhone !== '' ? showPhoneFormatted(deposit.depositPhone) : 'Telefone não cadastrado'}
                    </p>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-2 justify-end">
                      <button
                        type="button"
                        onClick={() => {
                          setForm({ depositId: stockist.depositId.toString(), userId: stockist.userId });
                          setModalOpen(true);
                        }}
                        className="rounded-lg border border-slate-200 dark:border-zinc-700 p-2 text-sm font-semibold text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors">
                        <i className="pi pi-pencil text-sm" />
                      </button>
                      <button
                        type="button"
                        onClick={() => setDeleteId(stockist.stockistId ?? null)}
                        className="rounded-lg border border-neutral-200 dark:border-zinc-700 p-2 text-sm font-semibold text-red-600 hover:bg-red-50 transition-colors"
                      >
                        <i className="pi pi-trash text-sm" />
                      </button>
                    </div>
                  </TableCell>
                </TableRow>
              )
            })}
          </TableBody>
        </Table>

      )}

      <EmbeddedDocPanel
        open={docOpen}
        onClose={() => setDocOpen(false)}
        title="Cadastro de Estoquistas"
        description="Guia para cadastrar responsáveis por almoxarifados e estruturar o controle de estoque."
        url="https://lumosip.com.br/como-usar/04-stock/01-stockist-management/"
      />

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} confirmation>
        <ModalHeader title="Vincular Estoquista" onClose={() => setModalOpen(false)} />
        <ModalBody className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Almoxarifado*</label>
            <GlassListbox
              searchable={true}
              value={form.depositId}
              onChange={(value) => setForm((previous) => ({ ...previous, depositId: value ?? '' }))}
              placeholder="Selecione o almoxarifado"
              options={[
                ...deposits.map((deposit) => ({
                  value: String(deposit.idDeposit),
                  label: deposit.depositName,
                })),
              ]}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Usuário*</label>
            <GlassListbox
              value={form.userId}
              onChange={(value) => setForm((previous) => ({ ...previous, userId: value ?? '' }))}
              placeholder="Selecione o usuário"
              searchable={true}
              options={[
                ...users
                  .filter((user) => {
                    const roles = user.role.map((role) => role.roleName);
                    return roles.includes('ESTOQUISTA')
                      || roles.includes('ESTOQUISTA_CHEFE')
                      || roles.includes('ADMIN');
                  })
                  .sort((a, b) => a.name.localeCompare(b.name))
                  .map((managedUser) => ({
                    value: managedUser.userId,
                    label: `${managedUser.name} ${managedUser.lastname}`,
                  })),
              ]}
            />
          </div>
        </ModalBody>
        <ModalFooter>
          <button
            type="button"
            onClick={() => setModalOpen(false)}
            className="rounded-xl border border-slate-200 dark:border-zinc-700 px-4 py-2 text-sm font-semibold text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors"
          >
            Cancelar
          </button>
          <button
            type="button"
            disabled={!form.depositId || !form.userId || saveMutation.isPending}
            onClick={() =>
              saveMutation.mutate(
                { depositId: form.depositId, userId: form.userId },
                {
                  onSuccess: () => {
                    notify('Estoquista cadastrado.', 'success');
                    setModalOpen(false);
                    setForm({ depositId: '', userId: '' });
                  },
                  onError: (error: unknown) => {
                    const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
                    notify(message ?? 'Erro ao cadastrar estoquista.', 'error');
                  },
                },
              )
            }
            className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 disabled:opacity-50 transition-colors"
          >
            {saveMutation.isPending && <i className="pi pi-spin pi-spinner mr-1.5 text-xs" />}
            Vincular
          </button>
        </ModalFooter>
      </Modal>

      <Modal open={deleteId !== null} onClose={() => setDeleteId(null)} confirmation>
        <ModalHeader title="Remover Estoquista" onClose={() => setDeleteId(null)} />
        <ModalBody>
          <p className="text-sm text-slate-600 dark:text-zinc-300">
            Confirma a remoção deste estoquista do almoxarifado?
          </p>
        </ModalBody>
        <ModalFooter>
          <button
            type="button"
            onClick={() => setDeleteId(null)}
            className="rounded-xl border border-slate-200 dark:border-zinc-700 px-4 py-2 text-sm font-semibold text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors"
          >
            Cancelar
          </button>
          <button
            type="button"
            disabled={deleteMutation.isPending}
            onClick={() =>
              deleteId !== null &&
              deleteMutation.mutate(deleteId, {
                onSuccess: () => {
                  notify('Estoquista removido.', 'success');
                  setDeleteId(null);
                },
                onError: (error: unknown) => {
                  const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
                  notify(message ?? 'Erro ao remover estoquista.', 'error');
                  setDeleteId(null);
                },
              })
            }
            className="rounded-xl bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-500 disabled:opacity-50 transition-colors"
          >
            {deleteMutation.isPending && <i className="pi pi-spin pi-spinner mr-1.5 text-xs" />}
            Remover
          </button>
        </ModalFooter>
      </Modal>
    </section>
  );
}
