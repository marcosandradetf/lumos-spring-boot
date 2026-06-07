import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';

import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { LoadingOverlay } from '@/shared/ui/loading-overlay';
import { AppDatePicker } from '@/shared/components/app-date-picker';
import { GlassListbox } from '@/shared/components/glass-list-box';
import { GlassMultiSelect } from '@/shared/components/glass-multi-select';
import { PdfPreviewModal } from '@/shared/components/pdf-preview-modal';
import type { MaterialTypeSubtype } from '@/features/stock/types/types';
import { reportsApi } from '@/features/reports/api/reportsApi';
import { materialApi } from '@/features/stock/api/material-api';
import { stockApi } from '@/features/stock/api/stock-api';

type ContractOption = { contractId: number; contractNumber?: string; number?: string; contractor: string };
type BrandOption = { brandId: number; brandName: string };
type Orientation = 'portrait' | 'landscape';

interface OperationFilters {
  contractIds: number[];
  materialTypesIds: number[];
  materialBrands: string[];
  startDate: Date | null;
  endDate: Date | null;
  orientation: Orientation;
}

const ORIENTATION_OPTIONS: Array<{ value: Orientation; label: string }> = [
  { value: 'portrait', label: 'Retrato' },
  { value: 'landscape', label: 'Paisagem' },
];

function getDefaultFilters(): OperationFilters {
  const now = new Date();
  return {
    contractIds: [],
    materialTypesIds: [],
    materialBrands: [],
    startDate: new Date(now.getFullYear(), now.getMonth(), 1),
    endDate: now,
    orientation: 'portrait',
  };
}

function diffInDays(start: Date, end: Date) {
  const startDate = new Date(start);
  const endDate = new Date(end);
  startDate.setHours(0, 0, 0, 0);
  endDate.setHours(0, 0, 0, 0);
  const ms = endDate.getTime() - startDate.getTime();
  return Math.floor(ms / (1000 * 60 * 60 * 24));
}

export default function OperationReport() {
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();

  const [filters, setFilters] = useState<OperationFilters>(getDefaultFilters);
  const [submitted, setSubmitted] = useState(false);
  const [pdfPreview, setPdfPreview] = useState<{ url: string; title: string } | null>(null);
  const generateOperationalReport = useMutation({
    mutationFn: reportsApi.generateOperationalReport,
  });

  useEffect(() => {
    setPageContext(['Relatórios', 'Execuções', 'Analítico de Operações'], 'Analítico de Operações');
  }, [setPageContext]);

  useEffect(() => () => {
    if (pdfPreview?.url) {
      URL.revokeObjectURL(pdfPreview.url);
    }
  }, [pdfPreview]);

  const closePdfPreview = () => {
    if (pdfPreview?.url) {
      URL.revokeObjectURL(pdfPreview.url);
    }
    setPdfPreview(null);
  };

  const { data: contractsRaw = [], isLoading: loadingContracts } = useQuery({
    queryKey: ['reports', 'contracts'],
    queryFn: reportsApi.getContracts,
  });
  const { data: types = [], isLoading: loadingTypes } = useQuery({
    queryKey: ['reports', 'material-types'],
    queryFn: stockApi.findAllTypeSubtype,
  });
  const { data: brands = [], isLoading: loadingBrands } = useQuery({
    queryKey: ['reports', 'material-brands'],
    queryFn: materialApi.getBrands,
  });

  const contracts = useMemo(() => {
    const map = new Map<number, ContractOption>();
    (contractsRaw as ContractOption[]).forEach((contract) => {
      if (!map.has(contract.contractId)) {
        map.set(contract.contractId, contract);
      }
    });
    return Array.from(map.values());
  }, [contractsRaw]);

  const handleReset = () => {
    setFilters(getDefaultFilters());
    setSubmitted(false);
  };

  const handleGenerate = async () => {
    setSubmitted(true);

    if (!filters.startDate || !filters.endDate || filters.contractIds.length === 0) {
      return;
    }

    const days = diffInDays(filters.startDate, filters.endDate);
    if (days > 93) {
      notify('O Período máximo é de 3 meses.', 'warn');
      return;
    }

    try {
      const blob = await generateOperationalReport.mutateAsync({
        contractIds: filters.contractIds,
        materialTypesIds: filters.materialTypesIds,
        materialBrands: filters.materialBrands,
        startDate: filters.startDate,
        endDate: filters.endDate,
        orientation: filters.orientation,
      });

      const url = URL.createObjectURL(blob);
      setPdfPreview((previous) => {
        if (previous?.url) {
          URL.revokeObjectURL(previous.url);
        }
        return { url, title: 'Relatório Analítico de Operações' };
      });
    } catch {
      notify('Erro ao gerar relatório.', 'error');
    }
  };

  const busy =
    generateOperationalReport.isPending ||
    loadingContracts ||
    loadingTypes ||
    loadingBrands;

  return (
    <section className="p-4 md:p-6 space-y-4 relative">
      <LoadingOverlay loading={busy} />

      <h1 className="text-2xl font-semibold text-slate-800 dark:text-zinc-100">Analítico de Operações</h1>

      <div className="rounded-2xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-900 dark:border-blue-900/40 dark:bg-blue-950/20 dark:text-blue-100">
        Apenas contratos e período são obrigatórios. Os demais filtros são opcionais e, quando não selecionados,
        consideram todos os registros.
      </div>

      <div className="rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900">
        <div className="border-b border-slate-200 px-6 py-4 dark:border-zinc-800">
          <h2 className="text-base font-semibold text-slate-900 dark:text-slate-100">Filtros</h2>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Refine os resultados por contratos, período, tipos de materiais e fornecedores.
          </p>
        </div>

        <div className="grid grid-cols-1 gap-5 p-6 sm:grid-cols-2 lg:grid-cols-3">
          <div className="flex flex-col gap-1">
            <label className="text-xs font-medium text-slate-700 dark:text-slate-300">
              Contratos <span className="text-red-500">*</span>
            </label>
            <GlassMultiSelect
              value={filters.contractIds}
              onChange={(value) => setFilters((previous) => ({ ...previous, contractIds: value.map(Number) }))}
              options={contracts.map((contract) => ({
                value: contract.contractId,
                label: `${contract.contractor} - Contrato N ${contract.contractNumber ?? contract.number ?? contract.contractId}`,
              }))}
              placeholder="Selecione os contratos"
              summaryMode="count"
              search
              searchPlaceholder="Pesquisar contratos..."
              emptyText="Nenhum contrato disponível"
            />
            {submitted && filters.contractIds.length === 0 && (
              <p className="text-xs text-red-500">Campo obrigatório.</p>
            )}
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-xs font-medium text-slate-700 dark:text-slate-300">
              Tipos de materiais <span className="text-slate-400">(opcional)</span>
            </label>
            <GlassMultiSelect
              value={filters.materialTypesIds}
              onChange={(value) => setFilters((previous) => ({ ...previous, materialTypesIds: value.map(Number) }))}
              options={(types as MaterialTypeSubtype[]).map((type) => ({
                value: type.typeId,
                label: type.typeName,
              }))}
              placeholder="Todos os tipos"
              summaryMode="count"
              search
              searchPlaceholder="Pesquisar tipos..."
              emptyText="Nenhum tipo disponível"
            />
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-xs font-medium text-slate-700 dark:text-slate-300">
              Fornecedores <span className="text-slate-400">(opcional)</span>
            </label>
            <GlassMultiSelect
              value={filters.materialBrands}
              onChange={(value) => setFilters((previous) => ({ ...previous, materialBrands: value.map(String) }))}
              options={(brands as BrandOption[]).map((brand) => ({
                value: brand.brandName,
                label: brand.brandName,
              }))}
              placeholder="Todos os fornecedores"
              summaryMode="count"
              search
              searchPlaceholder="Pesquisar fornecedores..."
              emptyText="Nenhum fornecedor disponível"
            />
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-xs font-medium text-slate-700 dark:text-slate-300">
              Data inicial <span className="text-red-500">*</span>
            </label>
            <AppDatePicker
              value={filters.startDate}
              onChange={(value) => setFilters((previous) => ({ ...previous, startDate: value }))}
              maxDate={filters.endDate ?? undefined}
              placeholder="Selecione a data"
            />
            {submitted && !filters.startDate && (
              <p className="text-xs text-red-500">Campo obrigatório.</p>
            )}
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-xs font-medium text-slate-700 dark:text-slate-300">
              Data final <span className="text-red-500">*</span>
            </label>
            <AppDatePicker
              value={filters.endDate}
              onChange={(value) => setFilters((previous) => ({ ...previous, endDate: value }))}
              minDate={filters.startDate ?? undefined}
              maxDate={new Date()}
              placeholder="Selecione a data"
            />
            {submitted && !filters.endDate && (
              <p className="text-xs text-red-500">Campo obrigatório.</p>
            )}
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-xs font-medium text-slate-700 dark:text-slate-300">Orientação do relatório</label>
            <GlassListbox
              value={filters.orientation}
              onChange={(value) => setFilters((previous) => ({ ...previous, orientation: value as Orientation }))}
              options={ORIENTATION_OPTIONS}
              placeholder="Selecione"
            />
          </div>
        </div>

        <div className="flex flex-col justify-end gap-3 border-t border-slate-200 px-6 py-4 dark:border-zinc-800 sm:flex-row">
          <button
            type="button"
            onClick={handleReset}
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50 dark:border-white/20 dark:text-slate-300 dark:hover:bg-white/5"
          >
            Limpar filtros
          </button>
          <button
            type="button"
            onClick={() => void handleGenerate()}
            disabled={busy}
            className="rounded-lg bg-blue-600 px-5 py-2 text-sm text-white transition hover:bg-blue-700 disabled:opacity-60"
          >
            Gerar Relatório
          </button>
        </div>
      </div>

      <PdfPreviewModal
        open={pdfPreview !== null}
        url={pdfPreview?.url ?? null}
        title={pdfPreview?.title ?? 'Visualizar PDF'}
        onClose={closePdfPreview}
      />
    </section>
  );
}
