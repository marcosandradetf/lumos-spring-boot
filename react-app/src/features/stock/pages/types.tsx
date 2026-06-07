import { useState, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { DataTable } from '@/shared/ui/data-table';
import type { Column } from '@/shared/ui/data-table';
import { Modal, ModalHeader, ModalBody, ModalFooter } from '@/shared/ui/modal';
import { GlassListbox } from '@/shared/components/glass-list-box';
import { stockApi } from '@/features/stock/api/stock-api';
import { stockKeys } from '@/features/stock/api/query-keys';
import { useTypes } from '@/features/stock/hooks/use-types';
import { useGroups } from '@/features/stock/hooks/use-groups';
import type { MaterialType, Group } from '@/features/stock/types/types';

interface TypeForm {
  typeName: string;
  groupId: number | null;
}

export default function Types() {
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();
  const queryClient = useQueryClient();

  const [modalOpen, setModalOpen] = useState(false);
  const [editType, setEditType] = useState<MaterialType | null>(null);
  const [form, setForm] = useState<TypeForm>({ typeName: '', groupId: null });
  const [deleteId, setDeleteId] = useState<number | null>(null);

  useEffect(() => {
    setPageContext(['Estoque', 'Tipos de Materiais'], 'Tipos de Materiais');
  }, [setPageContext]);

  const { data: types = [], isLoading } = useTypes();
  const { data: groups = [] } = useGroups();
  const saveMutation = useMutation({
    mutationFn: ({ editType, typeName, groupId }: { editType: MaterialType | null; typeName: string; groupId: number | null }) => {
      const payload = { typeName, groupId };
      if (editType) {
        return stockApi.updateType(editType.idType, payload);
      }
      return stockApi.insertType(payload);
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: stockKeys.types() }),
        queryClient.invalidateQueries({ queryKey: stockKeys.typesSubtype() }),
      ]);
    },
  });
  const deleteMutation = useMutation({
    mutationFn: (id: number) => stockApi.deleteType(id),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: stockKeys.types() }),
        queryClient.invalidateQueries({ queryKey: stockKeys.typesSubtype() }),
      ]);
    },
  });

  const openCreate = () => {
    setEditType(null);
    setForm({ typeName: '', groupId: null });
    setModalOpen(true);
  };

  const openEdit = (t: MaterialType) => {
    setEditType(t);
    setForm({ typeName: t.typeName, groupId: t.group?.idGroup ?? null });
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditType(null);
    setForm({ typeName: '', groupId: null });
  };

  const inputClass = 'w-full rounded-xl border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-3 py-2.5 text-sm text-slate-800 dark:text-zinc-100 outline-none focus:border-indigo-400 transition-colors';

  const columns: Column<MaterialType>[] = [
    { key: 'idType', header: '#', accessor: 'idType', cellClassName: 'w-16 text-slate-400 dark:text-zinc-500' },
    { key: 'typeName', header: 'Tipo', accessor: 'typeName' },
    {
      key: 'group',
      header: 'Grupo',
      render: (t) => (
        <span className="inline-flex rounded-full bg-slate-100 dark:bg-zinc-800 px-2.5 py-1 text-xs text-slate-600 dark:text-zinc-300">
          {t.group?.groupName ?? '—'}
        </span>
      ),
    },
    {
      key: 'actions',
      header: '',
      cellClassName: 'w-20',
      render: (t) => (
        <div className="flex gap-1">
          <button type="button" onClick={() => openEdit(t)}
            className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-indigo-50 hover:text-indigo-600 dark:hover:bg-indigo-900/20 dark:hover:text-indigo-400 transition-colors">
            <i className="pi pi-pencil text-xs" />
          </button>
          <button type="button" onClick={() => setDeleteId(t.idType)}
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
        <h1 className="text-2xl font-semibold text-slate-800 dark:text-zinc-100">Tipos de Materiais</h1>
        <button type="button" onClick={openCreate}
          className="flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 transition-colors">
          <i className="pi pi-plus text-sm" /> Novo Tipo
        </button>
      </div>

      <DataTable columns={columns} data={types as MaterialType[]} rowKey={t => t.idType}
        loading={isLoading} emptyMessage="Nenhum tipo cadastrado." />

      <Modal open={modalOpen} onClose={closeModal} confirmation>
        <ModalHeader title={editType ? 'Editar Tipo' : 'Novo Tipo'} onClose={closeModal} />
        <ModalBody className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Nome do tipo*</label>
            <input type="text" autoFocus value={form.typeName}
              onChange={(e) => setForm(prev => ({ ...prev, typeName: e.target.value }))}
              placeholder="Ex.: LÂMPADA LED" className={inputClass} />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Grupo</label>
            <GlassListbox
              value={form.groupId}
              onChange={(value) => setForm((prev) => ({ ...prev, groupId: value === null ? null : Number(value) }))}
              placeholder="Selecione um grupo"
              options={[
                { value: null, label: 'Selecione um grupo' },
                ...(groups as Group[]).map((group) => ({
                  value: group.idGroup,
                  label: group.groupName,
                })),
              ]}
            />
          </div>
        </ModalBody>
        <ModalFooter>
          <button type="button" onClick={closeModal}
            className="rounded-xl border border-slate-200 dark:border-zinc-700 px-4 py-2 text-sm font-semibold text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors">
            Cancelar
          </button>
          <button type="button" disabled={!form.typeName.trim() || saveMutation.isPending}
            onClick={() => saveMutation.mutate(
              { editType, typeName: form.typeName, groupId: form.groupId },
              {
                onSuccess: () => {
                  notify(editType ? 'Tipo atualizado.' : 'Tipo criado.', 'success');
                  closeModal();
                },
                onError: (error: unknown) => {
                  const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
                  notify(message ?? 'Erro ao salvar tipo.', 'error');
                },
              },
            )}
            className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 disabled:opacity-50 transition-colors">
            {saveMutation.isPending && <i className="pi pi-spin pi-spinner mr-1.5 text-xs" />}
            Salvar
          </button>
        </ModalFooter>
      </Modal>

      <Modal open={deleteId !== null} onClose={() => setDeleteId(null)} confirmation>
        <ModalHeader title="Excluir Tipo" onClose={() => setDeleteId(null)} />
        <ModalBody>
          <p className="text-sm text-slate-600 dark:text-zinc-300">
            Excluir este tipo removerá todos os subtypes associados. Confirma?
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
                notify('Tipo excluído.', 'success');
                setDeleteId(null);
              },
              onError: (error: unknown) => {
                const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
                notify(message ?? 'Erro ao excluir tipo.', 'error');
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
