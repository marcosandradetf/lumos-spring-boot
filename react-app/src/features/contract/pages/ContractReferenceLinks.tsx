import { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { LoadingOverlay } from '@/shared/ui/loading-overlay';
import { GlassMultiSelect } from '@/shared/components/glass-multi-select';
import { Toggle } from '@/shared/components/ui/toggle';
import { useCatalogue } from '@/features/stock/hooks/use-catalogue';
import { contractsApi } from '@/features/contract/api/contractsApi';
import { contractKeys } from '@/features/contract/api/contractQueryKeys';
import type { ContractReferenceItemManagementDTO, SaveContractReferenceItemLinksDTO } from '@/features/contract/types';
import type { MaterialFormDTO } from '@/features/stock/types/types';
import { Package } from 'lucide-react';

const DEPENDENCY_DRIVEN_TYPES = new Set(['SERVIÇO', 'PROJETO', 'BRAÇO']);
const NO_MATERIAL_TYPES = new Set(['SERVIÇO', 'PROJETO', 'MANUTENÇÃO', 'EXTENSÃO DE REDE', 'CEMIG']);

type Mode = 'item' | 'material';

interface EditableItem {
  contractReferenceItemId: number;
  description: string;
  type: string | null;
  status: string;
  materialIds: number[];
  dependencyIds: number[];
}

function normalize(value: string): string {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();
}

function getStatusKind(status: string): 'ACTIVE' | 'PENDING_VALIDATION' {
  return normalize(status).startsWith('active') ? 'ACTIVE' : 'PENDING_VALIDATION';
}

function getStatusLabel(status: string): string {
  return getStatusKind(status) === 'ACTIVE' ? 'Ativo' : 'Pendente';
}

function parseDependencyStatusTerm(status: string): string | null {
  const normalized = normalize(status);
  if (!normalized.includes('com ')) {
    return null;
  }

  const [, rawTerm] = normalized.split('com ');
  const term = rawTerm?.trim();
  if (!term || term === 'item' || term === 'itens') {
    return null;
  }

  return term;
}

function mapToEditable(dto: ContractReferenceItemManagementDTO): EditableItem {
  return {
    contractReferenceItemId: Number(dto.contractReferenceItemId),
    description: dto.description,
    type: dto.type,
    status: dto.status,
    materialIds: [...new Set(dto.materialLinks.map((material) => Number(material.materialId)).filter(Boolean))],
    dependencyIds: [...new Set(dto.dependencyLinks.map((dep) => Number(dep.contractReferenceItemId)).filter(Boolean))],
  };
}

export default function ContractReferenceLinks() {
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const queryClient = useQueryClient();

  const mode: Mode = (searchParams.get('operation') as Mode) === 'item' ? 'item' : 'material';

  const [items, setItems] = useState<EditableItem[]>([]);
  const [materialIds, setMaterialIds] = useState<Record<number, number[]>>({});
  const [depIds, setDepIds] = useState<Record<number, number[]>>({});
  const [activeId, setActiveId] = useState<number | null>(null);
  const [saving, setSaving] = useState<number | 'all' | null>(null);
  const [itemSearch, setItemSearch] = useState('');
  const [showOnlyUnlinked, setShowOnlyUnlinked] = useState(false);

  useEffect(() => {
    setPageContext(
      ['Contratos', mode === 'item' ? 'Vincular Itens Referenciais' : 'Vincular Materiais'],
      mode === 'item' ? 'Vínculo de Itens Referenciais' : 'Vínculo de Materiais a Itens',
    );
  }, [mode, setPageContext]);

  const { data: referenceItems, isLoading: loadingItems } = useQuery({
    queryKey: contractKeys.referenceLinks(),
    queryFn: contractsApi.getReferenceItemLinkManagement,
  });

  const { data: catalogue, isLoading: loadingMaterials } = useCatalogue(true);

  const loading = loadingItems || loadingMaterials;

  useEffect(() => {
    if (!referenceItems) {
      return;
    }

    const mapped = referenceItems.map(mapToEditable);
    setItems(mapped);

    const nextMaterialIds: Record<number, number[]> = {};
    const nextDepIds: Record<number, number[]> = {};

    mapped.forEach((item) => {
      nextMaterialIds[item.contractReferenceItemId] = [...item.materialIds];
      nextDepIds[item.contractReferenceItemId] = [...item.dependencyIds];
    });

    setMaterialIds(nextMaterialIds);
    setDepIds(nextDepIds);
  }, [referenceItems]);

  const itemModeItems = useMemo(
    () => items.filter((item) => item.type && DEPENDENCY_DRIVEN_TYPES.has(item.type)),
    [items],
  );

  const materialModeItems = useMemo(
    () => items.filter((item) => !item.type || !NO_MATERIAL_TYPES.has(item.type)),
    [items],
  );

  const currentItems = mode === 'item' ? itemModeItems : materialModeItems;

  const filteredCurrentItems = useMemo(() => {
    const term = normalize(itemSearch);

    return currentItems.filter((item) => {
      const matchesSearch = !term || normalize(item.description).includes(term);
      if (!matchesSearch) return false;

      if (!showOnlyUnlinked) return true;

      const linkCount = mode === 'item'
        ? (depIds[item.contractReferenceItemId] ?? []).length
        : (materialIds[item.contractReferenceItemId] ?? []).length;

      return linkCount === 0;
    });
  }, [currentItems, depIds, itemSearch, materialIds, mode, showOnlyUnlinked]);

  const dependencyOptions = useMemo(
    () => items.map((item) => ({
      value: item.contractReferenceItemId,
      label: item.description,
      type: item.type,
    })),
    [items],
  );

  const materials = (catalogue as MaterialFormDTO[] | undefined) ?? [];
  const materialOptions = useMemo(
    () => materials
      .filter((material) => Number(material.materialId) > 0)
      .map((material) => ({
        value: Number(material.materialId),
        label: `${material.materialName}${material.requestUnit ? ` (${material.requestUnit})` : ''}`,
      })),
    [materials],
  );

  const buildPayload = useCallback(
    (item: EditableItem): SaveContractReferenceItemLinksDTO => ({
      contractReferenceItemId: item.contractReferenceItemId,
      materialIds: materialIds[item.contractReferenceItemId] ?? [],
      dependencyReferenceItemIds: depIds[item.contractReferenceItemId] ?? [],
    }),
    [depIds, materialIds],
  );

  const applyResponse = useCallback(
    (response: ContractReferenceItemManagementDTO[]) => {
      const byId = new Map(response.map((entry) => [Number(entry.contractReferenceItemId), entry]));
      setItems((previous) => previous.map((item) => {
        const saved = byId.get(item.contractReferenceItemId);
        return saved ? mapToEditable(saved) : item;
      }));
      void queryClient.invalidateQueries({ queryKey: contractKeys.referenceLinks() });
    },
    [queryClient],
  );

  const saveMutation = useMutation({
    mutationFn: (payload: SaveContractReferenceItemLinksDTO[]) => contractsApi.saveReferenceItemLinks(payload),
  });

  const handleSaveItem = (item: EditableItem) => {
    setSaving(item.contractReferenceItemId);
    saveMutation.mutate([buildPayload(item)], {
      onSuccess: (response, payloads) => {
        applyResponse(response as ContractReferenceItemManagementDTO[]);
        const count = payloads.length;
        notify(`${count} ${count === 1 ? 'item salvo' : 'itens salvos'}.`, 'success');
        setSaving(null);
      },
      onError: (error: unknown) => {
        const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
        notify(message ?? 'Erro ao salvar vínculos.', 'error');
        setSaving(null);
      },
    });
  };

  const handleSaveAll = () => {
    if (currentItems.length === 0) {
      notify('Nenhum item para salvar.', 'warn');
      return;
    }

    setSaving('all');
    saveMutation.mutate(currentItems.map(buildPayload), {
      onSuccess: (response, payloads) => {
        applyResponse(response as ContractReferenceItemManagementDTO[]);
        const count = payloads.length;
        notify(`${count} ${count === 1 ? 'item salvo' : 'itens salvos'}.`, 'success');
        setSaving(null);
      },
      onError: (error: unknown) => {
        const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
        notify(message ?? 'Erro ao salvar vínculos.', 'error');
        setSaving(null);
      },
    });
  };

  const guideNeeded =
    (mode === 'item' && itemModeItems.length === 0 && !loading) ||
    (mode === 'material' && (materialModeItems.length === 0 || materials.length === 0) && !loading);

  const STATUS_BADGE = {
    ACTIVE: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300',
    PENDING_VALIDATION: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300',
  };

  const getDependencyOptionsForItem = (item: EditableItem) => {
    const requiredTypeTerm = parseDependencyStatusTerm(item.status);

    return dependencyOptions
      .filter((option) => option.value !== item.contractReferenceItemId)
      .filter((option) => {
        if (!requiredTypeTerm) {
          return true;
        }

        return normalize(option.type ?? '').includes(requiredTypeTerm);
      });
  };

  const getStatusMessage = (item: EditableItem) => {
    if (item.type && DEPENDENCY_DRIVEN_TYPES.has(item.type)) {
      return `${item.type} exige item de referência vinculado.`;
    }

    return 'Este tipo não exige item vinculado, mas a relação pode ser mantida aqui.';
  };

  return (
    <section className="p-4 md:p-6 max-w-5xl mx-auto space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-slate-800 dark:text-zinc-100">
            {mode === 'item' ? 'Vínculo de Itens Referenciais' : 'Vínculo de Materiais a Itens'}
          </h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-zinc-400 max-w-xl">
            {mode === 'item'
              ? 'Vincule serviços e projetos aos itens de dependência (ex: Braço → Poste).'
              : 'Associe materiais do catálogo a cada item referencial.'}
          </p>
        </div>

        {/* <div className="flex rounded-xl border border-slate-200 dark:border-zinc-700 overflow-hidden text-sm font-medium">
          {(['material', 'item'] as Mode[]).map((optionMode) => (
            <button
              key={optionMode}
              type="button"
              onClick={() => setMode(optionMode)}
              className={`px-4 py-2 transition-colors ${mode === optionMode ? 'bg-indigo-600 text-white' : 'text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800'}`}
            >
              {optionMode === 'item' ? (
                <>
                  <i className="pi pi-link mr-1.5 text-xs" />Itens
                </>
              ) : (
                <>
                  <i className="pi pi-box mr-1.5 text-xs" />Materiais
                </>
              )}
            </button>
          ))}
        </div> */}
      </div>

      {guideNeeded && (
        <div className="rounded-2xl border border-amber-200 dark:border-amber-900/40 bg-amber-50 dark:bg-amber-950/20 p-5 space-y-3">
          <div className="flex items-center gap-2">
            <i className="pi pi-info-circle text-amber-600" />
            <h2 className="text-sm font-semibold text-amber-800 dark:text-amber-200">Antes de editar os vínculos</h2>
          </div>
          <ul className="list-disc pl-5 text-sm text-amber-700 dark:text-amber-300 space-y-1">
            {itemModeItems.length === 0 && (
              <li>Cadastre itens no catálogo de referência com tipos: SERVIÇO, PROJETO ou BRAÇO.</li>
            )}
            {mode === 'material' && materials.length === 0 && (
              <li>Cadastre materiais no catálogo de estoque.</li>
            )}
          </ul>
          <div className="flex gap-2 flex-wrap">
            <button
              type="button"
              onClick={() => navigate('/contratos/itens-contratuais/cadastro')}
              className="flex items-center gap-1.5 rounded-xl bg-amber-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-amber-500 transition-colors"
            >
              <i className="pi pi-list text-xs" /> Ir para Cadastro de Itens
            </button>
            {mode === 'material' && materials.length === 0 && (
              <button
                type="button"
                onClick={() => navigate('/estoque/cadastrar-material')}
                className="flex items-center gap-1.5 rounded-xl border border-amber-300 dark:border-amber-700 px-3 py-1.5 text-xs font-semibold text-amber-800 dark:text-amber-200 hover:bg-amber-100 dark:hover:bg-amber-900/20 transition-colors"
              >
                <i className="pi pi-box text-xs" /> Cadastrar Materiais
              </button>
            )}
          </div>
        </div>
      )}

      {!guideNeeded && (
        <>
          <div className="rounded-2xl border border-emerald-200 dark:border-emerald-900/40 bg-emerald-50 dark:bg-emerald-950/20 px-4 py-3 text-sm text-emerald-800 dark:text-emerald-200 flex items-center gap-2">
            <i className="pi pi-info-circle text-sm" />
            Edite os vínculos diretamente na linha. Salve individualmente ou use <strong>Salvar todos</strong>.
          </div>

          <div className="relative rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 overflow-hidden">
            <LoadingOverlay loading={loading} />

            <div className="flex flex-col gap-3 px-4 py-3 border-b border-slate-100 dark:border-zinc-800 bg-slate-50 dark:bg-zinc-900/50 lg:flex-row lg:items-center lg:justify-between">
              <div className="min-w-0">
                <h2 className="text-sm font-semibold text-slate-800 dark:text-zinc-100">
                  {mode === 'item' ? 'Vínculos de itens' : 'Vínculos de materiais'}
                </h2>
                <p className="text-xs text-slate-500 dark:text-zinc-400 mt-0.5">
                  {filteredCurrentItems.length} de {currentItems.length} registro(s)
                </p>
              </div>
              <div className="flex w-full flex-col gap-2 sm:flex-row sm:flex-wrap sm:items-center lg:w-auto lg:justify-end">
                <div className="relative w-full sm:w-[260px]">
                  <i className="pi pi-search pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-xs text-slate-400 dark:text-zinc-500" />
                  <input
                    type="text"
                    value={itemSearch}
                    onChange={(event) => setItemSearch(event.target.value)}
                    placeholder="Buscar por nome"
                    className="h-9 w-full rounded-xl border border-slate-300 bg-white pl-8 pr-3 text-sm text-slate-700 outline-none transition-colors focus:border-indigo-400 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
                  />
                </div>

                <Toggle
                  variant="outline"
                  aria-label="Filtro: apenas itens sem vínculo"
                  pressed={showOnlyUnlinked}
                  onPressedChange={(pressed) => setShowOnlyUnlinked(pressed)}
                  className="h-9 w-full justify-start gap-2 border-slate-300 bg-white px-3 text-slate-700 hover:bg-slate-100 sm:w-auto sm:justify-center dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-200 dark:hover:bg-zinc-800 data-[state=on]:border-blue-300 data-[state=on]:bg-blue-50 data-[state=on]:text-blue-700 dark:data-[state=on]:border-blue-700 dark:data-[state=on]:bg-blue-900/30 dark:data-[state=on]:text-blue-300"
                >
                  <Package className="size-4" />
                  <span className="text-xs font-semibold uppercase tracking-wide">Filtro</span>
                  <span className="h-4 w-px bg-slate-200 dark:bg-zinc-700" />
                  <span className="text-sm font-medium">Sem vínculo</span>
                  <span
                    className={`ml-1 inline-flex min-w-[58px] justify-center rounded-full px-2 py-0.5 text-[10px] font-bold ${showOnlyUnlinked
                      ? 'bg-blue-100 text-blue-700 ring-1 ring-blue-300/70 dark:bg-blue-800/40 dark:text-blue-200'
                      : 'bg-slate-100 text-slate-500 dark:bg-zinc-800 dark:text-zinc-400'
                      }`}
                  >
                    {showOnlyUnlinked ? 'ATIVO' : 'INATIVO'}
                  </span>
                </Toggle>

                <button
                  type="button"
                  disabled={saveMutation.isPending}
                  onClick={handleSaveAll}
                  className="ml-auto flex h-9 w-full items-center justify-center gap-1.5 rounded-xl bg-indigo-600 px-4 text-xs font-semibold text-white hover:bg-indigo-500 disabled:opacity-50 transition-colors sm:w-auto"
                >
                  {saving === 'all' && saveMutation.isPending
                    ? <i className="pi pi-spin pi-spinner text-xs" />
                    : <i className="pi pi-save text-xs" />}
                  Salvar todos
                </button>
              </div>
            </div>

            <div className="divide-y divide-slate-100 dark:divide-zinc-800">
              {filteredCurrentItems.length === 0 && !loading && (
                <p className="px-4 py-8 text-center text-sm text-slate-400 dark:text-zinc-500">
                  Nenhum item encontrado com os filtros atuais.
                </p>
              )}

              {filteredCurrentItems.map((item) => {
                const isOpen = activeId === item.contractReferenceItemId;
                const currentMaterialIds = materialIds[item.contractReferenceItemId] ?? [];
                const currentDependencyIds = depIds[item.contractReferenceItemId] ?? [];
                const linkCount = mode === 'item' ? currentDependencyIds.length : currentMaterialIds.length;
                const isSavingThis = saving === item.contractReferenceItemId && saveMutation.isPending;
                const dependencyItemOptions = getDependencyOptionsForItem(item).map((option) => ({
                  value: option.value,
                  label: option.type ? `${option.label} (${option.type})` : option.label,
                }));
                const statusKind = getStatusKind(item.status);

                return (
                  <div key={item.contractReferenceItemId}>
                    <div className="flex items-center gap-3 px-4 py-3">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="text-sm font-medium text-slate-700 dark:text-zinc-200 truncate">{item.description}</span>
                          {item.type && (
                            <span className="inline-flex rounded-full bg-slate-100 dark:bg-zinc-800 px-2 py-0.5 text-xs text-slate-600 dark:text-zinc-300">
                              {item.type}
                            </span>
                          )}
                          <span className={`inline-flex rounded-full px-2 py-0.5 text-[11px] font-semibold ${STATUS_BADGE[statusKind]}`}>
                            {getStatusLabel(item.status)}
                          </span>
                        </div>
                        <p className="text-xs text-slate-500 dark:text-zinc-400 mt-0.5">
                          {linkCount === 0
                            ? (mode === 'item' ? 'Nenhum item vinculado' : 'Nenhum material vinculado')
                            : `${linkCount} ${mode === 'item' ? (linkCount === 1 ? 'item vinculado' : 'itens vinculados') : (linkCount === 1 ? 'material vinculado' : 'materiais vinculados')}`}
                        </p>
                      </div>

                      <div className="flex items-center gap-2 flex-shrink-0">
                        <button
                          type="button"
                          onClick={() => setActiveId(isOpen ? null : item.contractReferenceItemId)}
                          className="flex items-center gap-1.5 rounded-xl border border-slate-200 dark:border-zinc-700 px-3 py-1.5 text-xs font-medium text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors"
                        >
                          <i className={`pi ${isOpen ? 'pi-chevron-up' : 'pi-pencil'} text-xs`} />
                          {isOpen ? 'Fechar' : 'Editar'}
                        </button>
                        <button
                          type="button"
                          disabled={isSavingThis || saveMutation.isPending}
                          onClick={() => handleSaveItem(item)}
                          className="flex items-center gap-1.5 rounded-xl bg-slate-800 dark:bg-zinc-100 text-white dark:text-zinc-900 px-3 py-1.5 text-xs font-semibold hover:opacity-90 disabled:opacity-40 transition-all"
                        >
                          {isSavingThis ? <i className="pi pi-spin pi-spinner text-xs" /> : <i className="pi pi-save text-xs" />}
                          Salvar
                        </button>
                      </div>
                    </div>

                    {isOpen && (
                      <div className={`px-4 pb-4 ${mode === 'item' ? 'border-l-4 border-indigo-300 dark:border-indigo-700 ml-4' : 'border-l-4 border-amber-300 dark:border-amber-700 ml-4'} bg-slate-50/50 dark:bg-zinc-800/30`}>
                        {mode === 'item' ? (
                          <div className="space-y-2 pt-3">
                            <p className="text-xs font-medium text-slate-500 dark:text-zinc-400">Selecione os itens vinculados</p>
                            <GlassMultiSelect
                              value={currentDependencyIds}
                              onChange={(ids) => setDepIds((previous) => ({ ...previous, [item.contractReferenceItemId]: ids }))}
                              options={dependencyItemOptions}
                              placeholder="Sem itens vinculados"
                              search
                              searchPlaceholder="Pesquisar item referencial"
                              summaryMode="auto"
                              maxChips={1}
                            />
                            <p className="text-xs text-slate-500 dark:text-zinc-400">{getStatusMessage(item)}</p>
                          </div>
                        ) : (
                          <div className="space-y-2 pt-3">
                            <p className="text-xs font-medium text-slate-500 dark:text-zinc-400">Selecione os materiais</p>
                            <GlassMultiSelect
                              value={currentMaterialIds}
                              onChange={(ids) => setMaterialIds((previous) => ({ ...previous, [item.contractReferenceItemId]: ids }))}
                              options={materialOptions}
                              placeholder="Sem materiais vinculados"
                              search
                              searchPlaceholder="Pesquisar material"
                              initialSearch={item.type ?? ''}
                              summaryMode="count"
                            />
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </>
      )}
    </section>
  );
}
