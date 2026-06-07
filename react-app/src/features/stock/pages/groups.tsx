import { useState, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { DataTable } from '@/shared/ui/data-table';
import type { Column } from '@/shared/ui/data-table';
import { Modal, ModalHeader, ModalBody, ModalFooter } from '@/shared/ui/modal';
import { useGroups } from '@/features/stock/hooks/use-groups';
import { stockApi } from '@/features/stock/api/stock-api';
import { stockKeys } from '@/features/stock/api/query-keys';
import type { Group } from '@/features/stock/types/types';

export default function Groups() {
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();
  const queryClient = useQueryClient();

  const [modalOpen, setModalOpen] = useState(false);
  const [editGroup, setEditGroup] = useState<Group | null>(null);
  const [groupName, setGroupName] = useState('');
  const [deleteId, setDeleteId] = useState<number | null>(null);

  useEffect(() => {
    setPageContext(['Estoque', 'Grupos de Materiais'], 'Grupos de Materiais');
  }, [setPageContext]);

  const { data: groups = [], isLoading } = useGroups();
  const saveMutation = useMutation({
    mutationFn: ({ editGroup, groupName }: { editGroup: Group | null; groupName: string }) => {
      if (editGroup) {
        return stockApi.updateGroup(editGroup.idGroup, groupName);
      }

      return stockApi.insertGroup(groupName);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: stockKeys.groups() });
    },
  });
  const deleteMutation = useMutation({
    mutationFn: (id: number) => stockApi.deleteGroup(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: stockKeys.groups() });
    },
  });

  const openCreate = () => {
    setEditGroup(null);
    setGroupName('');
    setModalOpen(true);
  };

  const openEdit = (g: Group) => {
    setEditGroup(g);
    setGroupName(g.groupName);
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditGroup(null);
    setGroupName('');
  };

  const columns: Column<Group>[] = [
    { key: 'idGroup', header: '#', accessor: 'idGroup', cellClassName: 'w-16 text-slate-400 dark:text-zinc-500' },
    { key: 'groupName', header: 'Nome do Grupo', accessor: 'groupName' },
    {
      key: 'actions',
      header: '',
      cellClassName: 'w-20',
      render: (g) => (
        <div className="flex gap-1">
          <button type="button" onClick={() => openEdit(g)}
            className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-indigo-50 hover:text-indigo-600 dark:hover:bg-indigo-900/20 dark:hover:text-indigo-400 transition-colors">
            <i className="pi pi-pencil text-xs" />
          </button>
          <button type="button" onClick={() => setDeleteId(g.idGroup)}
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
        <h1 className="text-2xl font-semibold text-slate-800 dark:text-zinc-100">Grupos de Materiais</h1>
        <button type="button" onClick={openCreate}
          className="flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 transition-colors">
          <i className="pi pi-plus text-sm" /> Novo Grupo
        </button>
      </div>

      <DataTable columns={columns} data={groups as Group[]} rowKey={g => g.idGroup}
        loading={isLoading} emptyMessage="Nenhum grupo cadastrado." />

      {/* Create / Edit modal */}
      <Modal open={modalOpen} onClose={closeModal} confirmation>
        <ModalHeader title={editGroup ? 'Editar Grupo' : 'Novo Grupo'} onClose={closeModal} />
        <ModalBody>
          <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Nome do grupo</label>
          <input
            type="text"
            autoFocus
            value={groupName}
            onChange={(e) => setGroupName(e.target.value)}
            placeholder="Ex.: Cabos e Condutores"
            className="w-full rounded-xl border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-3 py-2.5 text-sm text-slate-800 dark:text-zinc-100 outline-none focus:border-indigo-400 transition-colors"
          />
        </ModalBody>
        <ModalFooter>
          <button type="button" onClick={closeModal}
            className="rounded-xl border border-slate-200 dark:border-zinc-700 px-4 py-2 text-sm font-semibold text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors">
            Cancelar
          </button>
          <button type="button" disabled={!groupName.trim() || saveMutation.isPending}
            onClick={() => saveMutation.mutate({ editGroup, groupName }, {
              onSuccess: () => {
                notify(editGroup ? 'Grupo atualizado.' : 'Grupo criado.', 'success');
                closeModal();
              },
              onError: (error: unknown) => {
                const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
                notify(message ?? 'Erro ao salvar grupo.', 'error');
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
        <ModalHeader title="Excluir Grupo" onClose={() => setDeleteId(null)} />
        <ModalBody>
          <p className="text-sm text-slate-600 dark:text-zinc-300">
            Tem certeza que deseja excluir este grupo? Esta ação não pode ser desfeita.
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
                notify('Grupo excluído.', 'success');
                setDeleteId(null);
              },
              onError: (error: unknown) => {
                const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
                notify(message ?? 'Erro ao excluir grupo.', 'error');
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
