import { useState, useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { DataTable } from '@/shared/ui/data-table';
import type { Column } from '@/shared/ui/data-table';
import { Modal, ModalBody, ModalFooter, ModalHeader } from '@/shared/ui/modal';
import { GlassListbox } from '@/shared/components/glass-list-box';
import { contractsApi } from '@/features/contract/api/contractsApi';
import { contractKeys } from '@/features/contract/api/contractQueryKeys';
import type { ContractReferenceItemBaseManagementDTO, SaveContractReferenceItemBaseDTO } from '@/features/contract/types';

const TYPES = [
  'BRAÇO',
  'REFLETOR',
  'LED',
  'PORCA',
  'FITA ISOLANTE ADESIVO',
  'POSTE',
  'EXTENSÃO DE REDE',
  'CINTA',
  'PARAFUSO',
  'FITA ISOLANTE AUTOFUSÃO',
  'CONECTOR',
  'POSTE GALVANIZADO',
  'CABO',
  'RELÉ',
  'SERVIÇO',
  'PROJETO',
  'CIMENTO',
  'POSTE CIMENTO',
  'MANUTENÇÃO',
];

interface FormState { description: string; type: string }

export default function ContractReferenceItems() {
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();
  const queryClient = useQueryClient();

  const [modalOpen, setModalOpen] = useState(false);
  const [editItem, setEditItem] = useState<ContractReferenceItemBaseManagementDTO | null>(null);
  const [form, setForm] = useState<FormState>({ description: '', type: '' });
  const [touched, setTouched] = useState(false);

  useEffect(() => {
    setPageContext(['Contratos', 'Catálogo de Itens Contratuais'], 'Catálogo de Itens Contratuais');
  }, [setPageContext]);

  const { data: items = [], isLoading } = useQuery({
    queryKey: contractKeys.referenceItemsBase(),
    queryFn: contractsApi.getReferenceItemBaseManagement,
  });
  const saveMutation = useMutation({
    mutationFn: (payload: SaveContractReferenceItemBaseDTO[]) => contractsApi.saveReferenceItemsBase(payload),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: contractKeys.referenceItemsBase() }),
        queryClient.invalidateQueries({ queryKey: contractKeys.referenceItems() }),
      ]);
    },
  });

  const closeModal = () => {
    setModalOpen(false);
    setEditItem(null);
    setForm({ description: '', type: '' });
    setTouched(false);
  };

  const STATUS_BADGE: Record<string, string> = {
    ACTIVE: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300',
    PENDING_VALIDATION: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300',
  };

  const inputClass = 'w-full rounded-xl border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-3 py-2.5 text-sm text-slate-800 dark:text-zinc-100 outline-none focus:border-indigo-400 transition-colors';

  const columns: Column<ContractReferenceItemBaseManagementDTO>[] = [
    { key: 'description', header: 'Descrição', accessor: 'description' },
    {
      key: 'type', header: 'Tipo',
      render: (i) => i.type ? (
        <span className="inline-flex rounded-full bg-slate-100 dark:bg-zinc-800 px-2.5 py-1 text-xs text-slate-600 dark:text-zinc-300">{i.type}</span>
      ) : <span className="text-slate-400 dark:text-zinc-500 text-xs">—</span>,
    },
    {
      key: 'status', header: 'Status',
      render: (i) => (
        <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${STATUS_BADGE[i.status] ?? ''}`}>
          {i.status === 'ACTIVE' ? 'Ativo' : 'Pendente'}
        </span>
      ),
    },
    {
      key: 'actions', header: '', cellClassName: 'w-14',
      render: (i) => (
        <button type="button" onClick={() => { setEditItem(i); setForm({ description: i.description, type: i.type ?? '' }); setModalOpen(true); }}
          className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-indigo-50 hover:text-indigo-600 dark:hover:bg-indigo-900/20 dark:hover:text-indigo-400 transition-colors">
          <i className="pi pi-pencil text-xs" />
        </button>
      ),
    },
  ];

  return (
    <section className="p-4 md:p-6 space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold text-slate-800 dark:text-zinc-100">Catálogo de Itens Contratuais</h1>
        <button type="button" onClick={() => setModalOpen(true)}
          className="flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 transition-colors">
          <i className="pi pi-plus text-sm" /> Novo Item
        </button>
      </div>

      <DataTable columns={columns} data={items as ContractReferenceItemBaseManagementDTO[]}
        rowKey={i => i.contractReferenceItemId ?? i.description} loading={isLoading}
        emptyMessage="Nenhum item cadastrado no catálogo." />

      <Modal open={modalOpen} onClose={closeModal} confirmation>
        <ModalHeader title={editItem ? 'Editar Item' : 'Novo Item Contratual'} onClose={closeModal} />
        <ModalBody className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Descrição*</label>
            <input type="text" autoFocus value={form.description}
              onChange={e => setForm(p => ({ ...p, description: e.target.value.toUpperCase() }))}
              placeholder="Ex.: Instalação de LED 100W" className={inputClass} />
            {touched && !form.description && (
              <p className="text-xs text-red-500 mt-1">Descrição é obrigatória.</p>
            )}
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-1">Tipo</label>
            <GlassListbox
              value={form.type}
              onChange={(value) => setForm((p) => ({ ...p, type: value ?? '' }))}
              placeholder="Selecione o tipo (opcional)"
              options={[
                { value: '', label: 'Selecione o tipo (opcional)' },
                ...TYPES.map((type) => ({ value: type, label: type })),
              ]}
            />
          </div>
        </ModalBody>
        <ModalFooter>
          <button type="button" onClick={closeModal}
            className="rounded-xl border border-slate-200 dark:border-zinc-700 px-4 py-2 text-sm font-semibold text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors">
            Cancelar
          </button>
          <button type="button" disabled={saveMutation.isPending}
            onClick={() => {
              setTouched(true);
              if (!form.description) {
                return;
              }
              const payload: SaveContractReferenceItemBaseDTO = {
                contractReferenceItemId: editItem?.contractReferenceItemId ?? null,
                description: form.description,
                type: form.type || null,
              };
              saveMutation.mutate([payload], {
                onSuccess: () => {
                  notify(editItem ? 'Item atualizado.' : 'Item criado.', 'success');
                  closeModal();
                },
                onError: (error: unknown) => {
                  const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
                  notify(message ?? 'Erro ao salvar item.', 'error');
                },
              });
            }}
            className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 disabled:opacity-50 transition-colors">
            {saveMutation.isPending && <i className="pi pi-spin pi-spinner mr-1.5 text-xs" />}
            Salvar
          </button>
        </ModalFooter>
      </Modal>
    </section>
  );
}
