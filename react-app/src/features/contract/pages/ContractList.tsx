import { Fragment, useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { Confirm } from '@/shared/components/confirm';
import { AppDatePicker } from '@/shared/components/app-date-picker';
import { AppNumberInput } from '@/shared/components/app-number-input';
import { GlassListbox } from '@/shared/components/glass-list-box';
import { LoadingOverlay } from '@/shared/ui/loading-overlay';
import { SkeletonTable } from '@/shared/ui/skeleton-table';
import { contractsApi } from '@/features/contract/api/contractsApi';
import { contractKeys } from '@/features/contract/api/contractQueryKeys';
import type {
  ContractFilters,
  ContractItemsResponseWithExecutionsSteps,
  ContractReferenceItemsDTO,
  ContractResponse,
} from '@/features/contract/types';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/shared/components/ui/tooltip';
import { CellEdit } from '@/shared/components/ui/cell-edit';
import { HoverCard, HoverCardTrigger, HoverCardContent } from '@/shared/components/ui/hover-card';

const fmtCurrency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const fmtDate = (value: string) => new Date(value).toLocaleDateString('pt-BR');

function defaultFilters(): ContractFilters {
  const endDate = new Date();
  const startDate = new Date();
  startDate.setMonth(startDate.getMonth() - 6);
  return {
    contractor: null,
    startDate,
    endDate,
    status: 'ACTIVE',
  };
}

function deepCopy<T>(value: T): T {
  if (typeof structuredClone === 'function') {
    return structuredClone(value);
  }

  return JSON.parse(JSON.stringify(value)) as T;
}

function mapItemForEdit(item: ContractItemsResponseWithExecutionsSteps): ContractReferenceItemsDTO {
  return {
    contractReferenceItemId: item.contractReferenceItemId,
    description: item.description,
    nameForImport: item.nameForImport ?? '',
    type: item.type,
    linking: item.linking ?? '',
    itemDependency: '',
    quantity: Number(item.contractedQuantity) || 0,
    price: Number(item.unitPrice) || 0,
    factor: Number(item.factor) || 1,
    contractItemId: item.contractItemId,
    totalExecuted: item.totalExecuted,
    executedQuantity: item.executedQuantity,
    reservedQuantity: item.reservedQuantity,
  };
}

function getTotalReserved(item: ContractItemsResponseWithExecutionsSteps): number {
  return (item.reservedQuantity ?? []).reduce((sum, row) => sum + (Number(row.quantity) || 0), 0);
}

function getMinimumContracted(item: ContractItemsResponseWithExecutionsSteps, defaultValue = 0): number {
  const value = Number(item.totalExecuted || 0) + getTotalReserved(item);
  return value === 0 ? defaultValue : value;
}

function normalizeExecutedQuantities(items: ContractItemsResponseWithExecutionsSteps[]) {
  const maxStep = items.reduce((max, item) => {
    const currentMax = Math.max(0, ...(item.executedQuantity ?? []).map((step) => Number(step.step) || 0));
    return Math.max(max, currentMax);
  }, 0);

  if (maxStep === 0) {
    return items;
  }

  return items.map((item) => {
    const source = item.executedQuantity ?? [];
    const normalized = Array.from({ length: maxStep }, (_, index) => {
      const step = index + 1;
      const found = source.find((entry) => entry.step === step);
      return found ?? { installationId: 0, step, quantity: 0 };
    });

    return {
      ...item,
      executedQuantity: normalized,
    };
  });
}

const STATUS_BADGE: Record<ContractResponse['contractStatus'], string> = {
  ACTIVE: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300',
  ARCHIVED: 'bg-slate-100 text-slate-600 dark:bg-zinc-800 dark:text-zinc-400',
};

export default function ContractList() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const queryClient = useQueryClient();

  const { setPageContext } = useAppStore();
  const { notify } = useNotify();

  const reason = (searchParams.get('for') ?? 'view').toLowerCase();

  const [filters, setFilters] = useState<ContractFilters>(defaultFilters);
  const [appliedFilters, setAppliedFilters] = useState<ContractFilters>(defaultFilters);

  const [selectedContract, setSelectedContract] = useState<ContractResponse | null>(null);
  const [showItems, setShowItems] = useState(false);
  const [currentContractId, setCurrentContractId] = useState<number>(0);

  const [deleteId, setDeleteId] = useState<number | null>(null);

  const [overlayLoading, setOverlayLoading] = useState(false);
  const [contractItems, setContractItems] = useState<ContractItemsResponseWithExecutionsSteps[]>([]);
  const [contractItemsBackup, setContractItemsBackup] = useState<ContractItemsResponseWithExecutionsSteps[]>([]);

  const [quantityEditing, setQuantityEditing] = useState<string | null>(null);
  const [priceEditing, setPriceEditing] = useState<string | null>(null);

  useEffect(() => {
    setPageContext(['Contratos', 'Listar Contratos'], 'Listar Contratos');
  }, [setPageContext]);

  const { data: contracts = [], isLoading } = useQuery({
    queryKey: contractKeys.list(appliedFilters),
    queryFn: () => contractsApi.getAllContracts(appliedFilters),
  });
  const archiveMutation = useMutation({
    mutationFn: (contractId: number) => contractsApi.archiveById(contractId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: contractKeys.all });
    },
  });
  const deleteMutation = useMutation({
    mutationFn: (contractId: number) => contractsApi.deleteById(contractId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: contractKeys.all });
    },
  });
  const updateContractItemsMutation = useMutation({
    mutationFn: ({ contractId, items }: { contractId: number; items: ContractReferenceItemsDTO[] }) => contractsApi.updateItems(items, contractId),
    onSuccess: async (_, variables) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: contractKeys.all }),
        queryClient.invalidateQueries({ queryKey: contractKeys.items(variables.contractId) }),
      ]);
    },
  });

  const fetchContractItemsWithSteps = (contractId: number) => queryClient.fetchQuery({
    queryKey: contractKeys.items(contractId),
    queryFn: () => contractsApi.getContractItemsWithExecutionsSteps(contractId),
  });

  const hasDiff = useMemo(
    () => JSON.stringify(contractItems) !== JSON.stringify(contractItemsBackup),
    [contractItems, contractItemsBackup],
  );

  const totalContracted = useMemo(
    () => contractItems.reduce((sum, item) => sum + Number(item.unitPrice || 0) * Number(item.contractedQuantity || 0), 0),
    [contractItems],
  );

  const inputClass = 'rounded-xl border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-3 py-2 text-sm text-slate-800 dark:text-zinc-100 outline-none focus:border-indigo-400 transition-colors';

  const loadItems = async (contract: ContractResponse, shouldShow = true) => {
    if (contract.contractId === 0) {
      setShowItems(shouldShow);
      return;
    }

    if (currentContractId === contract.contractId && contractItems.length > 0) {
      setShowItems(shouldShow);
      setSelectedContract(contract);
      return;
    }

    setOverlayLoading(true);
    setSelectedContract(contract);

    try {
      const items = await fetchContractItemsWithSteps(contract.contractId);
      const normalized = normalizeExecutedQuantities(items ?? []);
      setContractItems(normalized);
      setContractItemsBackup(deepCopy(normalized));
      setCurrentContractId(contract.contractId);
      setShowItems(shouldShow);
    } catch {
      notify('Erro ao carregar itens do contrato.', 'error');
    } finally {
      setOverlayLoading(!shouldShow);
    }
  };

  const handleEditContract = async (contract: ContractResponse) => {
    setOverlayLoading(true);

    try {
      await loadItems(contract, false);

      const payloadItems = contractItems.length > 0
        ? contractItems.map(mapItemForEdit)
        : (await fetchContractItemsWithSteps(contract.contractId)).map(mapItemForEdit);

      navigate(`/contratos/criar?contractId=${contract.contractId}`, {
        state: {
          contract,
          items: payloadItems,
          step: 1,
        },
      });
    } catch {
      notify('Erro ao carregar itens do contrato para edição.', 'error');
    } finally {
      setOverlayLoading(false);
    }
  };

  const handleContractClick = (contract: ContractResponse) => {
    if (reason === 'premeasurement') {
      navigate(`/pre-medicao/importar/contrato/${contract.contractId}`);
      return;
    }

    if (reason === 'execution') {
      navigate({
        pathname: '/ordens-de-servico/nova',
        search: `?codigo=${contract.contractId}&nome=${encodeURIComponent(contract.contractor)}`,
      });
      return;
    }

    setSelectedContract((previous) => previous?.contractId === contract.contractId ? null : contract);
    setShowItems(false);
  };

  const updateContractItem = (
    contractItemId: number,
    key: 'contractedQuantity' | 'unitPrice',
    value: number,
  ) => {
    setContractItems((previous) => previous.map((item) => {
      if (item.contractItemId !== contractItemId) {
        return item;
      }

      const nextValue = Number.isNaN(value) ? 0 : value;
      return {
        ...item,
        [key]: nextValue,
      };
    }));
  };

  const toggleSteps = (contractItemId: number) => {
    setContractItems((previous) => previous.map((item) => {
      if (item.contractItemId !== contractItemId) {
        return item;
      }

      return {
        ...item,
        showSteps: !item.showSteps,
      };
    }));
  };

  const deleteItem = (item: ContractItemsResponseWithExecutionsSteps) => {
    if (Number(item.totalExecuted || 0) + getTotalReserved(item) > 0) {
      notify('Não é permitido excluir item com execução ou reserva registrada.', 'warn');
      return;
    }

    setContractItems((previous) => previous.filter((row) => row.contractItemId !== item.contractItemId));
  };

  const cancelChanges = () => {
    setContractItems(deepCopy(contractItemsBackup));
    notify('Alterações descartadas.', 'info');
  };

  const validateContractItems = () => {
    const invalid = contractItems.some((item) => {
      const minQuantity = getMinimumContracted(item, 1);
      return Number(item.unitPrice || 0) <= 0 || Number(item.contractedQuantity || 0) < minQuantity;
    });

    if (invalid) {
      notify('Existem itens com valor unitário inválido ou quantidade abaixo do mínimo permitido.', 'warn');
      return false;
    }

    return true;
  };

  const saveContractItems = async () => {
    if (!selectedContract || !validateContractItems()) {
      return;
    }

    setOverlayLoading(true);

    try {
      const payload: ContractReferenceItemsDTO[] = contractItems.map((item) => ({
        contractReferenceItemId: item.contractReferenceItemId,
        description: item.description,
        nameForImport: item.nameForImport ?? '',
        type: item.type,
        linking: '',
        itemDependency: '',
        quantity: Number(item.contractedQuantity || 0),
        factor: Number(item.factor || 1),
        price: Number(item.unitPrice || 0),
        contractItemId: item.contractItemId,
        totalExecuted: item.totalExecuted,
        executedQuantity: item.executedQuantity,
        reservedQuantity: item.reservedQuantity,
      }));

      await updateContractItemsMutation.mutateAsync({
        contractId: selectedContract.contractId,
        items: payload,
      });
      setContractItemsBackup(deepCopy(contractItems));
      notify('Itens atualizados com sucesso.', 'success');
    } catch (error: unknown) {
      const message = (error as { response?: { data?: { message?: string; error?: string } } })?.response?.data?.message
        ?? (error as { response?: { data?: { error?: string } } })?.response?.data?.error
        ?? 'Erro ao salvar alterações de itens.';
      notify(message, 'error');
    } finally {
      setOverlayLoading(false);
    }
  };

  const showContractsList = !showItems;

  return (
    <section className="p-4 md:p-6 space-y-4 relative">
      <LoadingOverlay loading={overlayLoading} />

      {!showItems && (
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-2xl font-semibold text-slate-800 dark:text-zinc-100">
            {reason === 'execution' ? 'Selecionar Contrato para OS' : 'Contratos'}
          </h1>
          <button
            type="button"
            onClick={() => navigate('/contratos/criar')}
            className="flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 transition-colors"
          >
            <i className="pi pi-plus text-sm" /> Novo Contrato
          </button>
        </div>
      )}

      {showContractsList && (
        <>
          <div className="rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-4 flex flex-wrap gap-3 items-end">
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-500 dark:text-zinc-400">Data início</label>
              <AppDatePicker
                value={filters.startDate}
                onChange={(value) => setFilters((previous) => ({ ...previous, startDate: value }))}
                maxDate={filters.endDate ?? undefined}
                placeholder="Selecione a data"
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-500 dark:text-zinc-400">Data fim</label>
              <AppDatePicker
                value={filters.endDate}
                onChange={(value) => setFilters((previous) => ({ ...previous, endDate: value }))}
                minDate={filters.startDate ?? undefined}
                maxDate={new Date()}
                placeholder="Selecione a data"
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-500 dark:text-zinc-400">Status</label>
              <GlassListbox
                className="w-40"
                value={filters.status}
                onChange={(value) => setFilters((previous) => ({
                  ...previous,
                  status: (value ?? null) as ContractFilters['status'],
                }))}
                placeholder="Todos"
                options={[
                  { value: null, label: 'Todos' },
                  { value: 'ACTIVE', label: 'Ativo' },
                  { value: 'ARCHIVED', label: 'Arquivado' },
                ]}
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-500 dark:text-zinc-400">Contratante ou Nº contrato</label>
              <input
                type="text"
                className={inputClass}
                placeholder="Filtrar por contratante"
                value={filters.contractor ?? ''}
                onChange={(event) => setFilters((previous) => ({ ...previous, contractor: event.target.value || null }))}
              />
            </div>
            <button
              type="button"
              onClick={() => setAppliedFilters({ ...filters })}
              className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 transition-colors"
            >
              <i className="pi pi-search mr-1.5 text-xs" /> Buscar
            </button>
          </div>

          {isLoading ? (
            <SkeletonTable columns={5} />
          ) : contracts.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900">
              <i className="pi pi-file text-4xl text-slate-300 dark:text-zinc-600 mb-3" />
              <h3 className="text-base font-semibold text-slate-600 dark:text-zinc-300">Nenhum contrato encontrado</h3>
              <button
                type="button"
                onClick={() => navigate('/contratos/criar')}
                className="mt-4 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 transition-colors"
              >
                Cadastrar contrato
              </button>
            </div>
          ) : (
            <div className="space-y-2">
              {contracts.map((contract) => {
                const isSelected = selectedContract?.contractId === contract.contractId;

                return (
                  <div
                    key={contract.contractId}
                    className={`rounded-2xl border bg-white dark:bg-zinc-900 transition-all ${isSelected ? 'border-indigo-300 dark:border-indigo-700 shadow-md' : 'border-slate-200 dark:border-zinc-800'}`}
                  >
                    <div
                      onClick={() => handleContractClick(contract)}
                      className="flex cursor-pointer items-center justify-between gap-4 p-4 rounded-2xl transition-colors hover:bg-slate-50 dark:hover:bg-zinc-800/50"
                    >
                      <div className="flex items-start gap-3 min-w-0">
                        <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-xl bg-indigo-50 dark:bg-indigo-900/20">
                          <i className="pi pi-file-check text-indigo-600 dark:text-indigo-400" />
                        </div>
                        <div className="min-w-0">
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className="font-semibold text-sm text-slate-800 dark:text-zinc-100">{contract.contractor}</span>
                            <span className={`inline-flex rounded-full px-2 py-0.5 text-[11px] font-semibold ${STATUS_BADGE[contract.contractStatus]}`}>
                              {contract.contractStatus === 'ACTIVE' ? 'Ativo' : 'Arquivado'}
                            </span>
                          </div>
                          <div className="flex gap-3 flex-wrap text-xs text-slate-500 dark:text-zinc-400 mt-0.5">
                            <span>Nº {contract.number}</span>
                            <span>{fmtDate(contract.createdAt)}</span>
                            <span>{contract.itemQuantity} {contract.itemQuantity === 1 ? 'item' : 'itens'}</span>
                          </div>
                        </div>
                      </div>
                      <div className="flex items-center gap-3 flex-shrink-0">
                        <span className="font-semibold text-sm text-slate-700 dark:text-zinc-200 hidden md:block">
                          {contract.contractValue ? fmtCurrency.format(Number(contract.contractValue)) : '—'}
                        </span>
                        <i className={`pi ${isSelected ? 'pi-chevron-up' : 'pi-chevron-down'} text-slate-400 dark:text-zinc-500 text-xs`} />
                      </div>
                    </div>

                    {isSelected && reason === 'view' && (
                      <div className="border-t border-slate-100 dark:border-zinc-800 px-4 py-3 flex flex-wrap gap-2">
                        <button
                          type="button"
                          onClick={() => void loadItems(contract, true)}
                          className="flex items-center gap-1.5 rounded-xl border border-slate-200 dark:border-zinc-700 px-3 py-1.5 text-xs font-medium text-slate-600 dark:text-zinc-300 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors"
                        >
                          <i className="pi pi-list text-xs" /> Exibir Itens
                        </button>
                        <button
                          type="button"
                          onClick={() => void handleEditContract(contract)}
                          className="flex items-center gap-1.5 rounded-xl border border-slate-200 dark:border-zinc-700 px-3 py-1.5 text-xs font-medium text-slate-600 dark:text-zinc-300 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors"
                        >
                          <i className="pi pi-pencil text-xs" /> Editar Contrato
                        </button>
                        <button
                          type="button"
                          onClick={() => archiveMutation.mutate(contract.contractId, {
                            onSuccess: () => {
                              notify('Contrato arquivado/reativado.', 'success');
                              setSelectedContract(null);
                              setShowItems(false);
                            },
                            onError: (error: unknown) => {
                              const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
                              notify(message ?? 'Erro ao arquivar contrato.', 'error');
                            },
                          })}
                          className="flex items-center gap-1.5 rounded-xl border border-slate-200 dark:border-zinc-700 px-3 py-1.5 text-xs font-medium text-slate-600 dark:text-zinc-300 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors"
                        >
                          <i className={`pi ${contract.contractStatus === 'ACTIVE' ? 'pi-folder-open' : 'pi-inbox'} text-xs`} />
                          {contract.contractStatus === 'ACTIVE' ? 'Arquivar' : 'Reativar'}
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeleteId(contract.contractId)}
                          className="flex items-center gap-1.5 rounded-xl border border-red-200 dark:border-red-900/40 px-3 py-1.5 text-xs font-medium text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                        >
                          <i className="pi pi-trash text-xs" /> Excluir
                        </button>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </>
      )}

      {showItems && selectedContract && (
        <div className='space-y-2'>
          <div className="space-y-4">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 className="text-base font-semibold text-slate-900 dark:text-zinc-100">Itens do Contrato</h2>
                <p className="text-sm text-slate-600 dark:text-zinc-400">
                  {selectedContract.number} • {selectedContract.contractor}
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-2">
                {!hasDiff ? (
                  <>
                    <button
                      type="button"
                      onClick={() => setShowItems(false)}
                      className="rounded-xl border border-blue-200 dark:border-blue-900/40 px-3 py-1.5 text-xs font-semibold text-blue-700 dark:text-blue-300 hover:bg-blue-50 dark:hover:bg-blue-900/20 transition-colors"
                    >
                      <i className="pi pi-arrow-left mr-1 text-[10px]" /> Exibir Contratos
                    </button>
                    <button
                      type="button"
                      onClick={() => notify('Download de arquivo será portado na próxima etapa do módulo.', 'info')}
                      className="rounded-xl border border-slate-200 dark:border-zinc-700 px-3 py-1.5 text-xs font-semibold text-slate-700 dark:text-zinc-200 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors"
                    >
                      <i className="pi pi-download mr-1 text-[10px]" /> Arquivos do Contrato
                    </button>
                  </>
                ) : (
                  <>
                    <button
                      type="button"
                      onClick={cancelChanges}
                      className="rounded-xl border border-red-200 dark:border-red-900/40 px-3 py-1.5 text-xs font-semibold text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                    >
                      <i className="pi pi-times mr-1 text-[10px]" /> Cancelar Alterações
                    </button>
                    <button
                      type="button"
                      onClick={() => void saveContractItems()}
                      className="rounded-xl bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-emerald-500 transition-colors"
                    >
                      <i className="pi pi-check mr-1 text-[10px]" /> Salvar Alterações
                    </button>
                  </>
                )}
              </div>
            </div>
          </div>

          <div className="overflow-x-auto rounded-xl border border-slate-200 dark:border-zinc-700">
            <table className="w-full table-fixed text-sm">
              <colgroup>
                <col style={{ width: '6%' }} />
                <col style={{ width: '26%' }} />
                <col style={{ width: '7%' }} />
                <col style={{ width: '7%' }} />
                <col style={{ width: '7%' }} />
                <col style={{ width: '7%' }} />
                <col style={{ width: '9%' }} />
                <col style={{ width: '9%' }} />
                <col style={{ width: '9%' }} />
                <col style={{ width: '6%' }} />
                <col style={{ width: '7%' }} />
              </colgroup>
              <thead className="bg-slate-50 dark:bg-zinc-800/60 text-xs uppercase tracking-wide text-slate-500 dark:text-zinc-400">
                <tr>
                  <th className="w-[6%] px-3 py-2 text-left">Item</th>
                  <th className="w-[26%] px-3 py-2 text-left">Descrição</th>
                  <th className="w-[7%] px-3 py-2 text-right">
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <div className="truncate" title=''>
                          Qtde.
                        </div>
                      </TooltipTrigger>

                      <TooltipContent side="top" align="start" className="max-w-xs break-words">
                        <p>Quantidade Contratada</p>
                      </TooltipContent>
                    </Tooltip>
                  </th>
                  <th className="w-[7%] px-3 py-2 text-right">
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <div className="truncate" title=''>
                          Exec.
                        </div>
                      </TooltipTrigger>

                      <TooltipContent side="top" align="start" className="max-w-xs break-words">
                        <p>Total Executado</p>
                      </TooltipContent>
                    </Tooltip>
                  </th>
                  <th className="w-[7%] px-3 py-2 text-right">
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <div className="truncate" title=''>
                          Res.
                        </div>
                      </TooltipTrigger>

                      <TooltipContent side="top" align="start" className="max-w-xs break-words">
                        <p>Total Reservado</p>
                      </TooltipContent>
                    </Tooltip>
                  </th>
                  <th className="w-[7%] px-3 py-2 text-right">Saldo</th>
                  <th className="w-[9%] px-3 py-2 text-right">
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <div className="truncate" title=''>
                          Unit. R$
                        </div>
                      </TooltipTrigger>

                      <TooltipContent side="top" align="start" className="max-w-xs break-words">
                        <p>Valor Unitário</p>
                      </TooltipContent>
                    </Tooltip>
                  </th>
                  <th className="w-[9%] px-3 py-2 text-right">Total</th>
                  <th className="w-[9%] px-3 py-2 text-right">Saldo R$</th>
                  <th className="w-[6%] px-3 py-2 text-center">
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <div title=''>
                          Etapas
                        </div>
                      </TooltipTrigger>

                      <TooltipContent side="top" align="start" className="max-w-xs break-words">
                        <p>Etapas Concluídas</p>
                      </TooltipContent>
                    </Tooltip>
                  </th>
                  <th className="w-[7%] px-3 py-2 text-center">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-zinc-800">
                {contractItems.map((item) => {
                  const reserved = getTotalReserved(item);
                  const minQuantity = getMinimumContracted(item, 1);
                  const total = Number(item.unitPrice || 0) * Number(item.contractedQuantity || 0);
                  const balanceQty = Number(item.contractedQuantity || 0) - Number(item.totalExecuted || 0);
                  const balanceValue = Number(item.unitPrice || 0) * balanceQty;

                  return (
                    <Fragment key={`${item.contractItemId}-fragment`}>
                      <tr className="hover:bg-slate-50 dark:hover:bg-zinc-800/40 cursor-pointer transition-colors">
                        <td className="px-3 py-2 font-medium text-slate-700 dark:text-zinc-200">{item.number}</td>
                        <td className="w-[26%] px-3 py-2 max-w-[200px] text-slate-700 dark:text-zinc-200" title=''>
                          <Tooltip>
                            {/* asChild faz o gatilho herdar o comportamento sem criar uma tag extra na tabela */}
                            <TooltipTrigger asChild>
                              <div className="truncate" title=''>
                                {item.description}
                              </div>
                            </TooltipTrigger>

                            <TooltipContent side="top" align="start" className="max-w-xs break-words">
                              <p>{item.description}</p>
                            </TooltipContent>
                          </Tooltip>
                        </td>
                        <td className="px-3 py-2 text-right">
                          {quantityEditing === `q${item.contractItemId}` ? (
                            <AppNumberInput
                              value={Number(item.contractedQuantity || 0)}
                              onChange={(next) => updateContractItem(item.contractItemId, 'contractedQuantity', next)}
                              min={minQuantity}
                              minFractionDigits={0}
                              maxFractionDigits={2}
                              mode="decimal"
                              inputClassName="w-32"
                            />
                          ) : (
                            <CellEdit onClick={() => {
                              setQuantityEditing(`q${item.contractItemId}`);
                              setPriceEditing(null);
                            }}>
                              {item.contractedQuantity}
                            </CellEdit>
                          )}
                        </td>
                        <td className="px-3 py-2 text-right">
                          <HoverCard openDelay={100} closeDelay={100}>
                            {/* asChild faz o Radix usar o nosso span como o nó oficial do DOM */}
                            <HoverCardTrigger asChild>
                              <span className="cursor-pointer font-medium text-slate-700 hover:text-blue-600 transition-colors dark:text-zinc-200 dark:hover:text-blue-400">
                                {item.totalExecuted}
                              </span>
                            </HoverCardTrigger>
                            
                            {/* Customização direta para o Tailwind v4 garantir o fundo sólido e bordas finas */}
                            <HoverCardContent 
                              side="bottom" 
                              align="center" 
                              className="z-50 flex w-64 flex-col gap-1 rounded-xl border border-slate-200 bg-white p-3 shadow-lg outline-none dark:border-zinc-800 dark:bg-zinc-950 text-left text-xs"
                            >
                              <div className="flex flex-col gap-2 min-w-[180px] text-sm text-slate-800 dark:text-zinc-100">
                                
                                {!item.executedQuantity || item.executedQuantity.length === 0 ? (
                                  <span className="text-slate-400 dark:text-zinc-500 py-1 italic block text-center">
                                    Nenhuma execução registrada
                                  </span>
                                ) : (
                                  item.executedQuantity.map((step, index) => (
                                    <div 
                                      key={`${item.contractItemId}-step-${index}`} 
                                      className="flex justify-between items-center py-0.5 border-b border-slate-50 last:border-0 dark:border-zinc-900"
                                    >
                                      <span className="text-slate-500 dark:text-zinc-400 text-xs">
                                        {index + 1}ª Etapa
                                      </span>
                                      <span className="font-semibold text-slate-700 dark:text-zinc-200">
                                        {step.quantity}
                                      </span>
                                    </div>
                                  )) 
                                )}
                              </div>
                            </HoverCardContent>
                          </HoverCard>
                        </td>
                        <td className="px-3 py-2 text-right">
                          <HoverCard openDelay={100} closeDelay={100}>
                            {/* asChild faz o Radix usar o nosso span como o nó oficial do DOM */}
                            <HoverCardTrigger asChild>
                              <span className="cursor-pointer font-medium text-slate-700 hover:text-blue-600 transition-colors dark:text-zinc-200 dark:hover:text-blue-400">
                              {reserved}
                              </span>
                            </HoverCardTrigger>
                            
                            {/* Customização direta para o Tailwind v4 garantir o fundo sólido e bordas finas */}
                            <HoverCardContent 
                              side="bottom" 
                              align="center" 
                              className="z-50 flex w-64 flex-col gap-1 rounded-xl border border-slate-200 bg-white p-3 shadow-lg outline-none dark:border-zinc-800 dark:bg-zinc-950 text-left text-xs"
                            >
                              <div className="flex flex-col gap-2 min-w-[180px] text-sm text-slate-800 dark:text-zinc-100">
                                
                                {!item.reservedQuantity || item.reservedQuantity.length === 0 ? (
                                  <span className="text-slate-400 dark:text-zinc-500 py-1 italic block text-center">
                                    Nenhuma reserva registrada
                                  </span>
                                ) : (
                                  item.reservedQuantity.map((step, index) => (
                                    <div 
                                      key={`${item.contractItemId}-step-${index}`} 
                                      className="flex justify-between items-center py-0.5 border-b border-slate-50 last:border-0 dark:border-zinc-900"
                                    >
                                      <span className="text-slate-500 dark:text-zinc-400 text-xs">
                                        {index + 1}ª Etapa
                                      </span>
                                      <span className="font-semibold text-slate-700 dark:text-zinc-200">
                                        {step.quantity}
                                      </span>
                                    </div>
                                  )) 
                                )}
                              </div>
                            </HoverCardContent>
                          </HoverCard>
                        </td>
                        <td className="px-3 py-2 text-right">{balanceQty}</td>
                        <td className="px-3 py-2 text-right">
                          {priceEditing === `p${item.contractItemId}` ? (

                            <AppNumberInput
                              value={Number(item.unitPrice || 0)}
                              onChange={(next) => updateContractItem(item.contractItemId, 'unitPrice', next)}
                              min={0}
                              minFractionDigits={2}
                              maxFractionDigits={2}
                              mode="currency"
                              inputClassName="w-24"
                            />
                          ) : (
                            <CellEdit onClick={() => {
                              setPriceEditing(`p${item.contractItemId}`);
                              setQuantityEditing(null);
                            }}>
                              {fmtCurrency.format(Number(item.unitPrice || 0))}
                            </CellEdit>
                          )}
                        </td>
                        <td className="px-3 py-2 text-right font-semibold">{fmtCurrency.format(total)}</td>
                        <td className="px-3 py-2 text-right">{fmtCurrency.format(balanceValue)}</td>
                        <td className="px-3 py-2 text-center">
                          <button
                            type="button"
                            onClick={() => toggleSteps(item.contractItemId)}
                            className="rounded-lg border border-slate-200 dark:border-zinc-700 px-2 py-1 text-xs text-slate-600 dark:text-zinc-300 hover:bg-slate-50 dark:hover:bg-zinc-800"
                          >
                            <i className={`pi ${item.showSteps ? 'pi-chevron-up' : 'pi-list'} text-[10px]`} />
                          </button>
                        </td>
                        <td className="px-3 py-2 text-center">
                          <button
                            type="button"
                            onClick={() => deleteItem(item)}
                            className="rounded-lg border border-red-200 dark:border-red-900/40 px-2 py-1 text-xs text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20"
                          >
                            <i className="pi pi-trash text-[10px]" />
                          </button>
                        </td>
                      </tr>
                      {item.showSteps ? (
                        <tr key={`${item.contractItemId}-steps`} className="bg-slate-50/60 dark:bg-zinc-800/40">
                          <td colSpan={11} className="px-4 py-3">
                            <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
                              {(item.executedQuantity ?? []).map((stepItem, index) => (
                                <div key={`${item.contractItemId}-${stepItem.step}-${index}`} className="rounded-lg border border-slate-200 dark:border-zinc-700 px-3 py-2">
                                  <p className="text-[11px] text-slate-500 dark:text-zinc-400">{index + 1}ª etapa</p>
                                  <p className="text-sm font-semibold text-slate-700 dark:text-zinc-200">{stepItem.quantity}</p>
                                </div>
                              ))}
                              {(item.executedQuantity ?? []).length === 0 && (
                                <p className="text-xs text-slate-500 dark:text-zinc-400">Nenhuma execução registrada.</p>
                              )}
                            </div>
                          </td>
                        </tr>
                      ) : null}
                    </Fragment>
                  );
                })}
              </tbody>
              <tfoot className="border-t-2 border-slate-200 dark:border-zinc-700 bg-slate-50/60 dark:bg-zinc-800/40">
                <tr>
                  <td colSpan={7} className="px-3 py-2 text-right text-xs font-semibold text-slate-500 dark:text-zinc-400">Total contratado</td>
                  <td className="px-3 py-2 text-right font-bold text-slate-800 dark:text-zinc-100">{fmtCurrency.format(totalContracted)}</td>
                  <td colSpan={3} />
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      )}

      <Confirm
        open={deleteId !== null}
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && deleteMutation.mutate(deleteId, {
          onSuccess: () => {
            notify('Contrato excluído.', 'success');
            setDeleteId(null);
            setShowItems(false);
            setSelectedContract(null);
          },
          onError: (error: unknown) => {
            const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
            notify(message ?? 'Não é possível excluir este contrato.', 'error');
            setDeleteId(null);
          },
        })}
        title="Excluir Contrato"
        description="Tem certeza que deseja excluir este contrato? Esta ação não pode ser desfeita."
        confirmLabel="Excluir"
        loading={deleteMutation.isPending}
        danger
      />
    </section>
  );
}
