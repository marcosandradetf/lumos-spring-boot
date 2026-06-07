import { useState, useEffect, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { LoadingOverlay } from '@/shared/ui/loading-overlay';
import { AppDatePicker } from '@/shared/components/app-date-picker';
import { PdfPreviewModal } from '@/shared/components/pdf-preview-modal';
import { reportsApi } from '@/features/reports/api/reportsApi';
import type { MaintenanceContract } from '@/features/reports/types/types';

const MAX_RANGE_DAYS = 90;

function addDays(date: Date, days: number) {
  const d = new Date(date);
  d.setDate(d.getDate() + days);
  return d;
}

const fmtDate = (s: string) => new Date(s).toLocaleDateString('pt-BR');
const inputClass = 'rounded-xl border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-3 py-2 text-sm text-slate-800 dark:text-zinc-100 outline-none focus:border-indigo-400 transition-colors';

export default function MaintenanceReport() {
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();

  const [startDate, setStartDate] = useState<Date | null>(new Date(new Date().setDate(new Date().getDate() - 90)));
  const [endDate, setEndDate] = useState<Date | null>(new Date());
  const [pdfLoading, setPdfLoading] = useState<string | null>(null);
  const [pdfPreview, setPdfPreview] = useState<{ url: string; title: string } | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [searchRange, setSearchRange] = useState<{ startDate: Date; endDate: Date } | null>(null);
  const pdfRequestRef = useRef<{ executionId: string; type: string } | null>(null);

  const finishedMaintenances = useQuery({
    queryKey: [
      'finished-maintenances',
      searchRange?.startDate.toISOString() ?? null,
      searchRange?.endDate.toISOString() ?? null,
    ],
    queryFn: () => reportsApi.getFinishedMaintenances(searchRange!.startDate, searchRange!.endDate),
    enabled: Boolean(searchRange),
  });
  const generateMaintenancePdf = useQuery({
    queryKey: ['maintenance-pdf'],
    queryFn: () => reportsApi.generateMaintenancePdf(
      pdfRequestRef.current!.executionId,
      pdfRequestRef.current!.type,
    ),
    enabled: false,
    staleTime: 0,
    gcTime: 0,
  });
  
  const data = (finishedMaintenances.data as MaintenanceContract[] | undefined) ?? [];
  const loading = finishedMaintenances.isFetching || generateMaintenancePdf.isFetching;

  useEffect(() => {
    setPageContext(['Relatórios', 'Manutenções'], 'Relatório de Manutenções');
  }, [setPageContext]);

  const maxStartDate = endDate ? addDays(endDate, -MAX_RANGE_DAYS) : undefined;

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

  const handleSearch = async () => {
    if (!startDate || !endDate) {
      notify('Informe o período para buscar.', 'warn');
      return;
    }
    setSearchRange({ startDate, endDate });
  };

  const handlePdf = async (executionId: string, type: string) => {
    setPdfLoading(executionId);
    try {
      pdfRequestRef.current = { executionId, type };
      const result = await generateMaintenancePdf.refetch();
      const blob = result.data;
      if (!blob) {
        notify('Erro ao gerar relatório PDF.', 'error');
        return;
      }
      const url = URL.createObjectURL(blob);
      setPdfPreview((previous) => {
        if (previous?.url) {
          URL.revokeObjectURL(previous.url);
        }
        return {
          url,
          title: 'Relatório de Manutenção',
        };
      });
    } catch {
      notify('Erro ao gerar relatório PDF.', 'error');
    } finally {
      setPdfLoading(null);
    }
  };

  useEffect(() => {
    if (!finishedMaintenances.isError) return;
    notify('Erro ao buscar manutenções.', 'error');
  }, [finishedMaintenances.isError, notify]);

  const filtered = data.filter(group =>
    !searchTerm || group.contract.contractor.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <section className="p-4 md:p-6 space-y-4 relative">
      <LoadingOverlay loading={loading} />
      <h1 className="text-2xl font-semibold text-slate-800 dark:text-zinc-100">Manutenções</h1>

      {/* Filters */}
      <div className="rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-4 flex flex-wrap gap-3 items-end">
        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-slate-500 dark:text-zinc-400">Data início</label>
          <AppDatePicker
            value={startDate}
            onChange={setStartDate}
            minDate={maxStartDate}
            maxDate={endDate ?? undefined}
            placeholder="Selecione a data"
          />
        </div>
        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-slate-500 dark:text-zinc-400">Data fim</label>
          <AppDatePicker
            value={endDate}
            onChange={setEndDate}
            minDate={startDate ?? undefined}
            maxDate={new Date()}
            placeholder="Selecione a data"
          />
        </div>
        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-slate-500 dark:text-zinc-400">Pesquisar</label>
          <input type="text" className={inputClass} placeholder="Filtrar por contratante..."
            value={searchTerm} onChange={e => setSearchTerm(e.target.value)} />
        </div>
        <button type="button" onClick={() => void handleSearch()} disabled={loading}
          className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 disabled:opacity-50 transition-colors">
          <i className="pi pi-search mr-1.5 text-xs" />Buscar
        </button>
      </div>

      {/* Results */}
      {filtered.length > 0 && (
        <div className="space-y-4">
          {filtered.map(group => (
            <div key={group.contract.contract_id} className="rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 overflow-hidden">
              <div className="px-4 py-3 border-b border-slate-100 dark:border-zinc-800 bg-slate-50 dark:bg-zinc-900/50">
                <h2 className="text-sm font-semibold text-slate-800 dark:text-zinc-100">{group.contract.contractor}</h2>
                <span className="text-xs text-slate-500 dark:text-zinc-400">Relatórios disponíveis: {group.executions.length}</span>
              </div>
              <div className="divide-y divide-slate-100 dark:divide-zinc-800">
                {group.executions.map(exec => (
                  <div key={exec.execution_id} className="px-4 py-3 flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="text-xs font-mono text-slate-500 dark:text-zinc-400">{group.contract.contractor}-</span>
                        <span className="text-xs text-slate-500 dark:text-zinc-400">{fmtDate(exec.date_of_visit)}</span>
                      </div>
                      <span className={'text-xs'}><i className={'pi pi-users text-xs text-slate-500 dark:text-zinc-400'}></i> Equipe registrada</span>
                      {exec.team.length > 0 && (
                        <p className="text-xs text-slate-500 dark:text-zinc-400 mt-0.5">
                          {exec.team.map(m => `${m.name} ${m.last_name}`).join(', ')}
                        </p>
                      )}
                    </div>
                    <div className="flex gap-2 flex-shrink-0">
                      {['DATA', 'LED'].map(type => (
                        <button key={type} type="button"
                          disabled={pdfLoading === exec.execution_id}
                          onClick={() => void handlePdf(exec.execution_id, type)}
                          className="flex items-center gap-1.5 rounded-xl border border-slate-200 dark:border-zinc-700 px-3 py-1.5 text-xs font-medium text-slate-600 dark:text-zinc-300 hover:bg-slate-50 dark:hover:bg-zinc-800 disabled:opacity-40 transition-colors">
                          {pdfLoading === exec.execution_id ? (
                            <i className="pi pi-spin pi-spinner text-xs" />
                          ) : (
                            <i className="pi pi-file-pdf text-xs text-red-500" />
                          )}
                          {type === 'DATA' ? 'Convencional' : 'Fotográfico'}
                        </button>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {!loading && data.length === 0 && searchTerm === '' && (
        <div className="flex flex-col items-center justify-center py-12 text-slate-400 dark:text-zinc-500">
          <i className="pi pi-search text-3xl mb-2 opacity-40" />
          <p className="text-sm">Selecione o período e clique em Buscar.</p>
        </div>
      )}

      {!loading && data.length > 0 && filtered.length === 0 && (
        <div className="flex flex-col items-center justify-center py-12 text-slate-400 dark:text-zinc-500">
          <i className="pi pi-inbox text-3xl mb-2 opacity-40" />
          <p className="text-sm">Nenhum resultado para o filtro aplicado.</p>
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
