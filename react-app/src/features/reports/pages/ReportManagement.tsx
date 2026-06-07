import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';

import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { LoadingOverlay } from '@/shared/ui/loading-overlay';
import { AppDatePicker } from '@/shared/components/app-date-picker';
import { GlassListbox } from '@/shared/components/glass-list-box';
import { PdfPreviewModal } from '@/shared/components/pdf-preview-modal';
import { reportsApi } from '@/features/reports/api/reportsApi';


type Scope = 'MAINTENANCE' | 'INSTALLATION';
type ViewMode = 'GROUP' | 'LIST';

interface ContractOption {
  contractId: number;
  contractor: string;
  type: Scope;
  contractNumber?: string;
}

interface ExecutionItem {
  execution_id: string | null;
  execution_type: string | null;
  streets: string[];
  date_of_visit: string;
  finished_at: string;
  team: Array<{ name: string; last_name: string; role: string }>;
}

interface ExecutionGroup {
  contract: {
    contract_id: number;
    contractor: string;
  };
  executions: ExecutionItem[];
}

interface Filters {
  contractId: number | null;
  type: string | null;
  startDate: Date | null;
  endDate: Date | null;
  viewMode: ViewMode;
  scope: Scope;
  executionId: string | null;
  executionType: string | null;
}

const SCOPE_OPTIONS = [
  { value: 'MAINTENANCE', label: 'Manutenção' },
  { value: 'INSTALLATION', label: 'Instalação' },
];

const VIEW_MODE_OPTIONS = [
  { value: 'GROUP', label: 'Agrupado (PDF)' },
  { value: 'LIST', label: 'Lista' },
];

const SERVICE_TYPES: Record<Scope, Array<{ value: string; label: string }>> = {
  MAINTENANCE: [
    { value: 'lampada', label: 'Manutenção Convencional' },
    { value: 'led', label: 'Manutenção em LEDs' },
  ],
  INSTALLATION: [
    { value: 'data', label: 'Instalação de LEDs' },
    { value: 'photo', label: 'Relatório fotográfico' },
  ],
};

function getInitialFilters(): Filters {
  const now = new Date();
  return {
    contractId: null,
    type: null,
    startDate: new Date(now.getFullYear(), now.getMonth(), 1),
    endDate: now,
    viewMode: 'GROUP',
    scope: 'MAINTENANCE',
    executionId: null,
    executionType: null,
  };
}

function diffInDays(start: Date, end: Date) {
  const s = new Date(start);
  const e = new Date(end);
  s.setHours(0, 0, 0, 0);
  e.setHours(0, 0, 0, 0);
  return Math.floor((e.getTime() - s.getTime()) / (1000 * 60 * 60 * 24));
}

export default function ReportManagement() {
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();
  const [searchParams] = useSearchParams();

  const [filters, setFilters] = useState<Filters>(getInitialFilters);
  const [submitted, setSubmitted] = useState(false);
  const [showMenu, setShowMenu] = useState(true);
  const [results, setResults] = useState<ExecutionGroup[]>([]);
  const [pdfPreview, setPdfPreview] = useState<{ url: string; title: string } | null>(null);

  const contractsQuery = useQuery({
    queryKey: ['reports', 'contracts'],
    queryFn: reportsApi.getContracts,
  });
  const groupedReportMutation = useMutation({
    mutationFn: reportsApi.generateExecutionGroupedReport,
  });
  const listReportMutation = useMutation({
    mutationFn: reportsApi.getExecutionReport,
  });

  useEffect(() => {
    setPageContext(['Relatórios', 'Execuções', 'Agrupados'], 'Gerenciamento de Execuções');
  }, [setPageContext]);

  useEffect(() => () => {
    if (pdfPreview?.url) URL.revokeObjectURL(pdfPreview.url);
  }, [pdfPreview]);

  useEffect(() => {
    const contractId = searchParams.get('contractId');
    const executionId = searchParams.get('executionId');
    const type = searchParams.get('type');
    const scope = searchParams.get('scope');
    const executionType = searchParams.get('executionType');

    if (!contractId || !executionId || !type || !scope || !executionType) return;

    setFilters((previous) => ({
      ...previous,
      contractId: Number(contractId),
      type,
      scope: scope === 'INSTALLATION' ? 'INSTALLATION' : 'MAINTENANCE',
      executionId,
      executionType,
      viewMode: 'GROUP',
    }));
  }, [searchParams]);

  const contracts = useMemo(() => {
    return ((contractsQuery.data ?? []) as ContractOption[]).map((item) => ({
      ...item,
      contractor: item.contractor,
    }));
  }, [contractsQuery.data]);

  const filteredContracts = useMemo(
    () => contracts.filter((contract) => contract.type === filters.scope),
    [contracts, filters.scope],
  );

  const busy = contractsQuery.isLoading || groupedReportMutation.isPending || listReportMutation.isPending;

  const resetFilters = (scope: Scope) => {
    const next = getInitialFilters();
    next.scope = scope;
    setFilters(next);
    setSubmitted(false);
    setResults([]);
    setShowMenu(true);
  };

  const closePdfPreview = () => {
    if (pdfPreview?.url) URL.revokeObjectURL(pdfPreview.url);
    setPdfPreview(null);
  };

  const openGroupedPdf = async (custom?: Partial<Filters>) => {
    const payload = {
      ...filters,
      ...custom,
      startDate: (custom?.startDate ?? filters.startDate) as Date,
      endDate: (custom?.endDate ?? filters.endDate) as Date,
      contractId: Number(custom?.contractId ?? filters.contractId),
      viewMode: 'GROUP',
    };

    const blob = await groupedReportMutation.mutateAsync(payload as unknown as Record<string, unknown>);
    const url = URL.createObjectURL(blob);

    setPdfPreview((previous) => {
      if (previous?.url) URL.revokeObjectURL(previous.url);
      return { url, title: 'Relatório de Execuções' };
    });

    setShowMenu(false);
  };

  const applyFilters = async () => {
    setSubmitted(true);

    if (!filters.contractId || !filters.type || !filters.startDate || !filters.endDate) {
      return;
    }

    if (filters.type === 'photo') {
      notify("Relatório fotográfico em desenvolvimento. Use 'Relatórios de Instalações (90 dias)'.", 'info');
      return;
    }

    const days = diffInDays(filters.startDate, filters.endDate);
    if (days > 31) {
      notify('O Período máximo é de 31 dias.', 'warn');
      return;
    }

    try {
      if (filters.viewMode === 'GROUP') {
        await openGroupedPdf();
        return;
      }

      const list = await listReportMutation.mutateAsync(filters as unknown as Record<string, unknown>);
      const normalized = (list ?? []) as ExecutionGroup[];

      if (normalized.length === 0) {
        notify('Não existe nenhum relatório para os filtros selecionados.', 'warn');
        return;
      }

      setResults(normalized);
      setShowMenu(false);
    } catch (error: unknown) {
      const message = (error as { response?: { data?: { message?: string; error?: string } } })?.response?.data;
      notify(message?.message ?? message?.error ?? 'Erro ao gerar relatório.', 'error');
    }
  };

  const selectedContract = filteredContracts.find((item) => item.contractId === filters.contractId);
  const selectedTypeLabel = SERVICE_TYPES[filters.scope].find((item) => item.value === filters.type)?.label ?? '';

  return (
    <section className="relative p-4 md:p-6">
      <LoadingOverlay loading={busy} />

      {!showMenu && (
        <div className="mb-4 flex justify-end">
          <button
            type="button"
            onClick={() => {
              closePdfPreview();
              setResults([]);
              setShowMenu(true);
            }}
            className="rounded-lg bg-slate-100 px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-200 dark:bg-white/5 dark:text-slate-300 dark:hover:bg-white/10"
          >
            <i className="pi pi-chevron-left mr-2 text-xs" />Mostrar filtros
          </button>
        </div>
      )}

      {showMenu && (
        <div className="mb-6 flex justify-center">
          <div className="inline-flex rounded-2xl border border-slate-200 bg-white p-1 dark:border-white/10 dark:bg-slate-900">
            {SCOPE_OPTIONS.map((option) => (
              <button
                key={option.value}
                type="button"
                onClick={() => {
                  const scope = option.value as Scope;
                  setFilters((previous) => ({ ...previous, scope }));
                  resetFilters(scope);
                }}
                className={[
                  'rounded-xl px-4 py-2 text-sm font-semibold transition',
                  filters.scope === option.value
                    ? 'bg-gradient-to-r from-blue-600 to-cyan-500 text-white'
                    : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800',
                ].join(' ')}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>
      )}

      {showMenu ? (
        <section className="rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-white/10 dark:bg-slate-900">
          <header className="border-b border-slate-200 px-6 py-4 dark:border-white/10">
            <h2 className="text-base font-semibold text-slate-900 dark:text-slate-100">Filtros</h2>
            <p className="text-sm text-slate-500 dark:text-slate-400">Refine os resultados por contrato, período e tipo de serviço.</p>
          </header>

          <div className="grid grid-cols-1 gap-5 p-6 sm:grid-cols-2 lg:grid-cols-5">
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-600 dark:text-slate-400">Contrato</label>
              <GlassListbox
                value={filters.contractId}
                onChange={(value) => setFilters((previous) => ({ ...previous, contractId: value ? Number(value) : null }))}
                options={[
                  { value: null, label: 'Selecione o contrato' },
                  ...filteredContracts.map((contract) => ({
                    value: contract.contractId,
                    label: `${contract.contractor} • Nº ${contract.contractNumber ?? contract.contractId}`,
                  })),
                ]}
                placeholder="Selecione o contrato"
              />
              {!filters.contractId && submitted && <p className="text-xs text-red-500">Filtro obrigatório.</p>}
            </div>

            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-600 dark:text-slate-400">Tipo de serviço</label>
              <GlassListbox
                value={filters.type}
                onChange={(value) => setFilters((previous) => ({ ...previous, type: value ? String(value) : null }))}
                options={SERVICE_TYPES[filters.scope]}
                placeholder="Selecione o tipo"
              />
              {!filters.type && submitted && <p className="text-xs text-red-500">Filtro obrigatório.</p>}
            </div>

            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-600 dark:text-slate-400">Data inicial</label>
              <AppDatePicker
                value={filters.startDate}
                onChange={(value) => setFilters((previous) => ({ ...previous, startDate: value }))}
                maxDate={filters.endDate ?? undefined}
              />
              {!filters.startDate && submitted && <p className="text-xs text-red-500">Filtro obrigatório.</p>}
            </div>

            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-600 dark:text-slate-400">Data final</label>
              <AppDatePicker
                value={filters.endDate}
                onChange={(value) => setFilters((previous) => ({ ...previous, endDate: value }))}
                minDate={filters.startDate ?? undefined}
                maxDate={new Date()}
              />
              {!filters.endDate && submitted && <p className="text-xs text-red-500">Filtro obrigatório.</p>}
            </div>

            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-600 dark:text-slate-400">Visualização</label>
              <GlassListbox
                value={filters.viewMode}
                onChange={(value) => setFilters((previous) => ({ ...previous, viewMode: (value as ViewMode) ?? 'GROUP' }))}
                options={VIEW_MODE_OPTIONS}
              />
            </div>
          </div>

          <footer className="flex flex-col justify-end gap-3 border-t border-slate-200 px-6 py-4 dark:border-white/10 sm:flex-row">
            <button
              type="button"
              onClick={() => resetFilters(filters.scope)}
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-600 hover:bg-slate-50 dark:border-white/20 dark:text-slate-300 dark:hover:bg-white/5"
            >
              Limpar filtros
            </button>

            <button
              type="button"
              onClick={() => void applyFilters()}
              className="rounded-lg bg-blue-600 px-5 py-2 text-sm text-white transition hover:bg-blue-700"
            >
              Aplicar filtros
            </button>
          </footer>
        </section>
      ) : (
        <div className="space-y-4">
          <div className="flex flex-col items-center gap-1 px-3 py-2">
            <h2 className="text-center text-base font-semibold text-slate-800 dark:text-slate-100 sm:text-lg">{selectedContract?.contractor ?? 'Relatório de Execuções'}</h2>
            <p className="text-center text-xs text-slate-500 dark:text-slate-400 sm:text-sm">
              Contrato nº <span className="font-medium text-slate-700 dark:text-slate-300">{selectedContract?.contractNumber ?? '-'}</span>
            </p>
            <span className="mt-1 inline-flex items-center rounded-md bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600 dark:bg-slate-800 dark:text-slate-300">
              {selectedTypeLabel || 'Tipo não informado'}
            </span>
          </div>

          {!pdfPreview && results.length > 0 && (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {results.flatMap((group) =>
                group.executions.map((execution) => (
                  <button
                    key={`${group.contract.contract_id}-${execution.execution_id}`}
                    type="button"
                    onClick={() => void openGroupedPdf({ executionId: execution.execution_id, executionType: execution.execution_type })}
                    className="rounded-2xl border border-slate-200 bg-white p-5 text-left shadow-sm transition hover:-translate-y-[1px] hover:shadow-md dark:border-white/10 dark:bg-slate-900"
                  >
                    <p className="text-sm font-medium text-slate-700 dark:text-slate-200">
                      Data da execução: {new Date(execution.date_of_visit).toLocaleDateString('pt-BR')}
                    </p>

                    <div className="mt-3 grid grid-cols-2 gap-3 text-sm">
                      <div>
                        <span className="text-xs text-slate-500 dark:text-slate-400">Pontos atendidos</span>
                        <p className="text-lg font-semibold text-slate-900 dark:text-white">{execution.streets.length}</p>
                      </div>
                      <div>
                        <span className="text-xs text-slate-500 dark:text-slate-400">Tempo médio</span>
                        <p className="text-lg font-semibold text-slate-900 dark:text-white">
                          {Math.max(0, (new Date(execution.finished_at).getTime() - new Date(execution.date_of_visit).getTime()) / 36e5).toFixed(1)}h
                        </p>
                      </div>
                    </div>

                    <div className="mt-3">
                      <span className="mb-1 block text-xs text-slate-500 dark:text-slate-400">Equipe executora</span>
                      <div className="flex flex-wrap gap-1.5">
                        {execution.team.map((member) => (
                          <span
                            key={`${execution.execution_id}-${member.name}-${member.last_name}`}
                            className="inline-flex items-center rounded-md bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-700 dark:bg-slate-800 dark:text-slate-300"
                          >
                            {member.name} {member.last_name}
                          </span>
                        ))}
                      </div>
                    </div>
                  </button>
                )),
              )}
            </div>
          )}
        </div>
      )}

      <PdfPreviewModal
        open={pdfPreview !== null}
        url={pdfPreview?.url ?? null}
        title={pdfPreview?.title ?? 'Visualizar PDF'}
        onClose={closePdfPreview}
      />
    </section>
  );
}
