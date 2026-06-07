import { useState, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { DataTable } from '@/shared/ui/data-table';
import type { Column } from '@/shared/ui/data-table';
import { Modal, ModalHeader, ModalBody, ModalFooter } from '@/shared/ui/modal';
import { useDeposits } from '@/features/stock/hooks/use-deposits';
import { stockApi } from '@/features/stock/api/stock-api';
import { stockKeys } from '@/features/stock/api/query-keys';
import type { Deposit } from '@/features/stock/types/types';

type DepositForm = Omit<Deposit, 'idDeposit' | 'teamName' | 'plateVehicle'>;
type DepositsMode = 'all' | 'fixed' | 'truck';

const EMPTY_FORM: DepositForm = {
  depositName: '',
  depositAddress: '',
  depositDistrict: '',
  depositCity: '',
  depositState: '',
  depositRegion: '',
  depositPhone: '',
  isTruck: false,
};

const inputClass = 'w-full rounded-xl border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-3 py-2 text-sm text-slate-800 dark:text-zinc-100 placeholder:text-slate-400 outline-none focus:border-indigo-400 transition-colors';

interface DepositsProps {
  mode?: DepositsMode;
}

export default function Deposits({ mode = 'fixed' }: DepositsProps) {
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();
  const queryClient = useQueryClient();

  const [modalOpen, setModalOpen] = useState(false);
  const [editDeposit, setEditDeposit] = useState<Deposit | null>(null);
  const [form, setForm] = useState<DepositForm>(EMPTY_FORM);
  const [deleteId, setDeleteId] = useState<number | null>(null);

  useEffect(() => {
    const title = mode === 'truck' ? 'Caminhões / Veículos' : 'Almoxarifados';
    setPageContext(['Estoque', title], title);
  }, [setPageContext, mode]);

  const { data: deposits = [], isLoading } = useDeposits();
  const filteredDeposits = (deposits as Deposit[]).filter((deposit) => {
    if (mode === 'truck') return deposit.isTruck;
    if (mode === 'fixed') return !deposit.isTruck;
    return true;
  });
  const saveMutation = useMutation({
    mutationFn: ({ editDeposit, form }: { editDeposit: Deposit | null; form: DepositForm }) => {
      if (editDeposit) {
        return stockApi.updateDeposit(editDeposit.idDeposit, form);
      }

      return stockApi.insertDeposit(form);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: stockKeys.deposits() });
    },
  });
  const deleteMutation = useMutation({
    mutationFn: (id: number) => stockApi.deleteDeposit(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: stockKeys.deposits() });
    },
  });

  const openCreate = () => {
    setEditDeposit(null);
    setForm({
      ...EMPTY_FORM,
      isTruck: mode === 'truck',
    });
    setModalOpen(true);
  };

  const openEdit = (d: Deposit) => {
    setEditDeposit(d);
    setForm({
      depositName: d.depositName,
      depositAddress: d.depositAddress,
      depositDistrict: d.depositDistrict,
      depositCity: d.depositCity,
      depositState: d.depositState,
      depositRegion: d.depositRegion,
      depositPhone: d.depositPhone,
      isTruck: d.isTruck,
    });
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditDeposit(null);
    setForm({
      ...EMPTY_FORM,
      isTruck: mode === 'truck',
    });
  };

  const set = (key: keyof DepositForm) => (value: string | boolean) =>
    setForm(prev => ({ ...prev, [key]: value }));

  const columns: Column<Deposit>[] = [
    { key: 'depositName', header: 'Nome', accessor: 'depositName' },
    { key: 'depositCity', header: 'Cidade', accessor: 'depositCity' },
    { key: 'depositState', header: 'Estado', accessor: 'depositState' },
    { key: 'depositPhone', header: 'Telefone', accessor: 'depositPhone' },
    {
      key: 'isTruck',
      header: 'Tipo',
      render: (d) => (
        <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${d.isTruck ? 'bg-sky-100 text-sky-700 dark:bg-sky-900/30 dark:text-sky-300' : 'bg-slate-100 text-slate-600 dark:bg-zinc-800 dark:text-zinc-300'}`}>
          {d.isTruck ? 'Caminhão' : 'Fixo'}
        </span>
      ),
    },
    {
      key: 'actions',
      header: '',
      cellClassName: 'w-20',
      render: (d) => (
        <div className="flex gap-1">
          <button type="button" onClick={() => openEdit(d)}
            className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-indigo-50 hover:text-indigo-600 dark:hover:bg-indigo-900/20 dark:hover:text-indigo-400 transition-colors">
            <i className="pi pi-pencil text-xs" />
          </button>
          <button type="button" onClick={() => setDeleteId(d.idDeposit)}
            className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-900/20 dark:hover:text-red-400 transition-colors">
            <i className="pi pi-trash text-xs" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <section className="p-4 md:p-6 space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold text-slate-800 dark:text-zinc-100">
          {mode === 'truck' ? 'Caminhões / Veículos' : 'Almoxarifados'}
        </h1>
        <button type="button" onClick={openCreate}
          className="flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 transition-colors">
          <i className="pi pi-plus text-sm" /> {mode === 'truck' ? 'Novo Caminhão' : 'Novo Almoxarifado'}
        </button>
      </div>

      <DataTable columns={columns} data={filteredDeposits} rowKey={d => d.idDeposit}
        loading={isLoading} emptyMessage={mode === 'truck' ? 'Nenhum caminhão cadastrado.' : 'Nenhum almoxarifado cadastrado.'} />

      {/* Create / Edit modal */}
      <Modal open={modalOpen} onClose={closeModal} className="max-w-2xl">
        <ModalHeader
          title={
            editDeposit
              ? mode === 'truck' ? 'Editar Caminhão' : 'Editar Almoxarifado'
              : mode === 'truck' ? 'Novo Caminhão' : 'Novo Almoxarifado'
          }
          onClose={closeModal}
        />
        <ModalBody className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Nome*</label>
              <input type="text" autoFocus value={form.depositName}
                onChange={(e) => set('depositName')(e.target.value)}
                placeholder="Ex.: Almoxarifado Central" className={inputClass} />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Endereço</label>
              <input type="text" value={form.depositAddress}
                onChange={(e) => set('depositAddress')(e.target.value)}
                placeholder="Rua, número" className={inputClass} />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Bairro</label>
              <input type="text" value={form.depositDistrict}
                onChange={(e) => set('depositDistrict')(e.target.value)}
                placeholder="Bairro" className={inputClass} />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Cidade</label>
              <input type="text" value={form.depositCity}
                onChange={(e) => set('depositCity')(e.target.value)}
                placeholder="Cidade" className={inputClass} />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Estado (UF)</label>
              <input type="text" maxLength={2} value={form.depositState}
                onChange={(e) => set('depositState')(e.target.value.toUpperCase())}
                placeholder="SP" className={inputClass} />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Região</label>
              <input type="text" value={form.depositRegion}
                onChange={(e) => set('depositRegion')(e.target.value)}
                placeholder="Região operacional" className={inputClass} />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Telefone</label>
              <input type="tel" value={form.depositPhone}
                onChange={(e) => set('depositPhone')(e.target.value)}
                placeholder="(11) 9 0000-0000" className={inputClass} />
            </div>
          </div>
          {mode === 'all' ? (
            <label className="flex items-center gap-2 cursor-pointer select-none">
              <input type="checkbox" checked={form.isTruck}
                onChange={(e) => set('isTruck')(e.target.checked)}
                className="w-4 h-4 rounded accent-indigo-600" />
              <span className="text-sm text-slate-700 dark:text-zinc-200">É um depósito de caminhão</span>
            </label>
          ) : (
            <p className="text-xs text-slate-500 dark:text-zinc-400">
              Tipo: <span className="font-semibold">{mode === 'truck' ? 'Caminhão / móvel' : 'Fixo'}</span>
            </p>
          )}
        </ModalBody>
        <ModalFooter>
          <button type="button" onClick={closeModal}
            className="rounded-xl border border-slate-200 dark:border-zinc-700 px-4 py-2 text-sm font-semibold text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors">
            Cancelar
          </button>
          <button type="button" disabled={!form.depositName.trim() || saveMutation.isPending}
            onClick={() => saveMutation.mutate({
              editDeposit,
              form: {
                ...form,
                isTruck: mode === 'truck' ? true : mode === 'fixed' ? false : form.isTruck,
              },
            }, {
              onSuccess: () => {
                notify(editDeposit ? 'Almoxarifado atualizado.' : 'Almoxarifado criado.', 'success');
                closeModal();
              },
              onError: (error: unknown) => {
                const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
                notify(message ?? 'Erro ao salvar almoxarifado.', 'error');
              },
            })}
            className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 disabled:opacity-50 transition-colors">
            {saveMutation.isPending && <i className="pi pi-spin pi-spinner mr-1.5 text-xs" />}
            Salvar
          </button>
        </ModalFooter>
      </Modal>

      {/* Delete confirmation */}
      <Modal open={deleteId !== null} onClose={() => setDeleteId(null)} confirmation>
        <ModalHeader title="Excluir Almoxarifado" onClose={() => setDeleteId(null)} />
        <ModalBody>
          <p className="text-sm text-slate-600 dark:text-zinc-300">
            Confirma a exclusão deste almoxarifado? Esta ação não pode ser desfeita.
          </p>
        </ModalBody>
        <ModalFooter>
          <button type="button" onClick={() => setDeleteId(null)}
            className="rounded-xl border border-slate-200 dark:border-zinc-700 px-4 py-2 text-sm font-semibold text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors">
            Cancelar
          </button>
          <button type="button" disabled={deleteMutation.isPending}
            onClick={() => deleteId !== null && deleteMutation.mutate(deleteId, {
              onSuccess: () => {
                notify('Almoxarifado excluído.', 'success');
                setDeleteId(null);
              },
              onError: (error: unknown) => {
                const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
                notify(message ?? 'Erro ao excluir almoxarifado.', 'error');
                setDeleteId(null);
              },
            })}
            className="rounded-xl bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-500 disabled:opacity-50 transition-colors">
            {deleteMutation.isPending && <i className="pi pi-spin pi-spinner mr-1.5 text-xs" />}
            Excluir
          </button>
        </ModalFooter>
      </Modal>
    </section>
  );
}
