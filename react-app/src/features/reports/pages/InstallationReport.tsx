import { useState, useEffect, useRef, useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { LoadingOverlay } from '@/shared/ui/loading-overlay';
import { AppDatePicker } from '@/shared/components/app-date-picker';
import { PdfPreviewModal } from '@/shared/components/pdf-preview-modal';
import { reportsApi } from '@/features/reports/api/reportsApi';
import type { InstallationContract } from '@/features/reports/types/types';

const inputClass = 'rounded-xl border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-3 py-2 text-sm text-slate-800 dark:text-zinc-100 outline-none focus:border-indigo-400 transition-colors';

export default function InstallationReport() {
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();
  const [searchParams] = useSearchParams();
  const hasHandledQueryRef = useRef(false);
  const queryStartDateParam = searchParams.get('startDate');
  const queryEndDateParam = searchParams.get('endDate');

  const [startDate, setStartDate] = useState<Date | null>(() => {
    if (!queryStartDateParam) return new Date(new Date().setDate(new Date().getDate() - 90));
    const parsed = new Date(queryStartDateParam);
    return Number.isNaN(parsed.getTime()) ? new Date(new Date().setDate(new Date().getDate() - 90)) : parsed;
  });
  const [endDate, setEndDate] = useState<Date | null>(() => {
    if (!queryEndDateParam) return new Date();
    const parsed = new Date(queryEndDateParam);
    return Number.isNaN(parsed.getTime()) ? new Date() : parsed;
  });
  const [pdfLoading, setPdfLoading] = useState<number | null>(null);
  const [pdfPreview, setPdfPreview] = useState<{ url: string; title: string } | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [searchRange, setSearchRange] = useState<{ startDate: Date; endDate: Date } | null>(null);
  const pdfRequestRef = useRef<{ installationId: number; installationType: string; type: string } | null>(null);

  const finishedInstallations = useQuery({
    queryKey: [
      'finished-installations',
      searchRange?.startDate.toISOString() ?? null,
      searchRange?.endDate.toISOString() ?? null,
    ],
    queryFn: () => reportsApi.getFinishedInstallations(searchRange!.startDate, searchRange!.endDate),
    enabled: Boolean(searchRange),
  });

  const generateInstallationPdf = useQuery({
    queryKey: ['installation-pdf'],
    queryFn: () => reportsApi.generateInstallationPdf(
      pdfRequestRef.current!.installationId,
      pdfRequestRef.current!.installationType,
      pdfRequestRef.current!.type,
    ),
    enabled: false,
    staleTime: 0,
    gcTime: 0,
  });
  
  const data = (finishedInstallations.data as InstallationContract[] | undefined) ?? [];
  const loading = finishedInstallations.isFetching || generateInstallationPdf.isFetching;

  useEffect(() => {
    setPageContext(['Relatórios', 'Instalações'], 'Relatório de Instalações');
  }, [setPageContext]);

  const handleSearch = async () => {
    if (!startDate || !endDate) { notify('Informe o período.', 'warn'); return; }
    setSearchRange({ startDate, endDate });
  };

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

  const handlePdf = useCallback(async (installationId: number, installationType: string, type: string) => {
    setPdfLoading(installationId);
    pdfRequestRef.current = { installationId, installationType, type };
    try {
      const result = await generateInstallationPdf.refetch();
      const blob = result.data;

      if (!blob) {
        notify('Erro ao gerar PDF.', 'error');
        return;
      }

      const url = URL.createObjectURL(blob);
      setPdfPreview((previous) => {
        if (previous?.url) {
          URL.revokeObjectURL(previous.url);
        }
        return {
          url,
          title:  'Relatório de Instalação',
        };
      });
    } catch {
      notify('Erro ao gerar PDF.', 'error');
    } finally {
      setPdfLoading(null);
    }
  }, [generateInstallationPdf, notify]);

  useEffect(() => {
    if (hasHandledQueryRef.current) return;

    const type = searchParams.get('type');
    const startDateParam = searchParams.get('startDate');
    const endDateParam = searchParams.get('endDate');
    const installationIdParam = searchParams.get('installationId');
    const installationTypeParam = searchParams.get('installationType');

    if (type !== 'data' || !startDateParam || !endDateParam || !installationIdParam || !installationTypeParam) {
      return;
    }

    const parsedStartDate = new Date(startDateParam);
    const parsedEndDate = new Date(endDateParam);
    const parsedInstallationId = Number(installationIdParam);

    if (
      Number.isNaN(parsedStartDate.getTime()) ||
      Number.isNaN(parsedEndDate.getTime()) ||
      Number.isNaN(parsedInstallationId)
    ) {
      notify('Parâmetros inválidos para geração automática do relatório.', 'warn');
      hasHandledQueryRef.current = true;
      return;
    }

    hasHandledQueryRef.current = true;

    void (async () => {
      try {
        setSearchRange({ startDate: parsedStartDate, endDate: parsedEndDate });
        await handlePdf(parsedInstallationId, installationTypeParam, type);
      } catch (error) {
        console.error('auto report flow error', error);
        notify('Não foi possível carregar os dados para gerar o relatório automático.', 'error');
      }
    })();
  }, [searchParams, handlePdf, notify]);

  useEffect(() => {
    if (!finishedInstallations.isError) return;
    notify('Erro ao buscar instalações.', 'error');
  }, [finishedInstallations.isError, notify]);

  const filtered = data.filter(g =>
    !searchTerm || g.contract.contractor.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <section className="p-4 md:p-6 space-y-4 relative">
      <LoadingOverlay loading={loading} />
      <h1 className="text-2xl font-semibold text-slate-800 dark:text-zinc-100">Instalações</h1>

      <div className="rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-4 flex flex-wrap gap-3 items-end">
        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-slate-500 dark:text-zinc-400">Data início</label>
          <AppDatePicker
            value={startDate}
            onChange={setStartDate}
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
          <input type="text" className={inputClass} placeholder="Filtrar contratante..."
            value={searchTerm} onChange={e => setSearchTerm(e.target.value)} />
        </div>
        <button type="button" onClick={() => void handleSearch()} disabled={loading}
          className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 disabled:opacity-50 transition-colors">
          <i className="pi pi-search mr-1.5 text-xs" />Buscar
        </button>
      </div>

      {filtered.length > 0 && (
        <div className="space-y-4">
          {filtered.map(group => (
            <div key={group.contract.contract_id} className="rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 overflow-hidden">
              <div className="px-4 py-3 border-b border-slate-100 dark:border-zinc-800 bg-slate-50 dark:bg-zinc-900/50">
                <h2 className="text-sm font-semibold text-slate-800 dark:text-zinc-100">{group.contract.contractor}</h2>
                <span className="text-xs text-slate-500 dark:text-zinc-400">Relatórios disponíveis: {group.steps.length}</span>
              </div>
              <div className="divide-y divide-slate-100 dark:divide-zinc-800">
                {group.steps.map(step => (
                  <div key={`${step.installation_id}-${step.installation_type}`} className="px-4 py-3 flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="text-xs font-mono text-slate-500 dark:text-zinc-400">{step.description}</span>
                        {/*<span className="text-xs text-slate-500 dark:text-zinc-400">{fmtDate(step.date_of_visit)}</span>*/}
                      </div>
                      <span className={'text-xs'}><i className={'pi pi-users text-xs text-slate-500 dark:text-zinc-400'}></i> Equipe registrada</span>
                      {step.team.length > 0 && (
                        <p className="text-xs text-slate-500 dark:text-zinc-400 mt-0.5">
                          {step.team.map(m => `${m.name} ${m.last_name}`).join(', ')}
                        </p>
                      )}
                    </div>
                    <div className="flex gap-2 flex-shrink-0">
                      {['data', 'photos'].map(type => (
                        <button key={type} type="button" disabled={pdfLoading === step.installation_id}
                          onClick={() => void handlePdf(step.installation_id, step.installation_type, type)}
                          className="flex items-center gap-1.5 rounded-xl border border-slate-200 dark:border-zinc-700 px-3 py-1.5 text-xs font-medium text-slate-600 dark:text-zinc-300 hover:bg-slate-50 dark:hover:bg-zinc-800 disabled:opacity-40 transition-colors">
                          {pdfLoading === step.installation_id ? (
                            <i className="pi pi-spin pi-spinner text-xs" />
                          ) : (
                            <i className="pi pi-file-pdf text-xs text-red-500" />
                          )}
                          {type === 'data' ? 'Comum' : 'Fotográfico'}
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

      {!loading && data.length === 0 && !searchTerm && (
        <div className="flex flex-col items-center justify-center py-12 text-slate-400 dark:text-zinc-500">
          <i className="pi pi-search text-3xl mb-2 opacity-40" />
          <p className="text-sm">Selecione o período e clique em Buscar.</p>
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
