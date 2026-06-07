import { useEffect, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';

import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { LoadingOverlay } from '@/shared/ui/loading-overlay';
import { AppDatePicker } from '@/shared/components/app-date-picker';
import { GlassListbox } from '@/shared/components/glass-list-box';
import { PdfPreviewModal } from '@/shared/components/pdf-preview-modal';
import { reportsApi } from '@/features/reports/api/reportsApi';

interface ContractOption {
  contractId: number;
  contractor: string;
  type: 'MAINTENANCE' | 'INSTALLATION';
  contractNumber?: string;
}

function defaultDates() {
  const endDate = new Date();
  const startDate = new Date();
  startDate.setMonth(startDate.getMonth() - 3);
  return { startDate, endDate };
}

function diffInDays(start: Date, end: Date) {
  const s = new Date(start);
  const e = new Date(end);
  s.setHours(0, 0, 0, 0);
  e.setHours(0, 0, 0, 0);
  return Math.floor((e.getTime() - s.getTime()) / (1000 * 60 * 60 * 24));
}

export default function MaterialReservationReport() {
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();

  const initialDates = useMemo(() => defaultDates(), []);
  const [contractId, setContractId] = useState<number | null>(null);
  const [startDate, setStartDate] = useState<Date | null>(initialDates.startDate);
  const [endDate, setEndDate] = useState<Date | null>(initialDates.endDate);
  const [submitted, setSubmitted] = useState(false);
  const [showMenu, setShowMenu] = useState(true);
  const [loading, setLoading] = useState(false);
  const [pdfPreview, setPdfPreview] = useState<{ url: string; title: string } | null>(null);

  const contractsQuery = useQuery({
    queryKey: ['reports', 'contracts'],
    queryFn: reportsApi.getContracts,
  });

  useEffect(() => {
    setPageContext(['Relatórios', 'Estoque', 'Saída/Saldo por instalação'], 'Saída/Saldo por instalação');
  }, [setPageContext]);

  useEffect(() => () => {
    if (pdfPreview?.url) URL.revokeObjectURL(pdfPreview.url);
  }, [pdfPreview]);

  const contracts = ((contractsQuery.data ?? []) as ContractOption[])
    .filter((contract) => contract.type === 'INSTALLATION');

  const selectedContract = contracts.find((contract) => contract.contractId === contractId);

  const closePdfPreview = () => {
    if (pdfPreview?.url) URL.revokeObjectURL(pdfPreview.url);
    setPdfPreview(null);
  };

  const resetFilters = () => {
    const dates = defaultDates();
    setContractId(null);
    setStartDate(dates.startDate);
    setEndDate(dates.endDate);
    setSubmitted(false);
    setShowMenu(true);
    closePdfPreview();
  };

  const applyFilters = async () => {
    setSubmitted(true);

    if (!startDate || !endDate) return;

    const days = diffInDays(startDate, endDate);
    if (days > 93) {
      notify('O Período máximo é de 93 dias.', 'warn');
      return;
    }

    setLoading(true);
    try {
      const blob = await reportsApi.generateMaterialReservationReport({
        contractId,
        startDate,
        endDate,
      });
      const url = URL.createObjectURL(blob);

      setPdfPreview((previous) => {
        if (previous?.url) URL.revokeObjectURL(previous.url);
        return { url, title: 'Saída/Saldo por instalação' };
      });

      setShowMenu(false);
    } catch {
      notify('Nenhum dado encontrado no período informado.', 'info');
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="relative p-4 md:p-6">
      <LoadingOverlay loading={loading || contractsQuery.isLoading} />

      {!showMenu && (
        <div className="mb-4 flex justify-end">
          <button
            type="button"
            onClick={() => {
              closePdfPreview();
              setShowMenu(true);
            }}
            className="rounded-lg bg-slate-100 px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-200 dark:bg-white/5 dark:text-slate-300 dark:hover:bg-white/10"
          >
            <i className="pi pi-chevron-left mr-2 text-xs" />Mostrar filtros
          </button>
        </div>
      )}

      {showMenu ? (
        <section className="rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-white/10 dark:bg-slate-900">
          <header className="border-b border-slate-200 px-6 py-4 dark:border-white/10">
            <h2 className="text-base font-semibold text-slate-900 dark:text-slate-100">Filtros</h2>
            <p className="text-sm text-slate-500 dark:text-slate-400">Refine os resultados por contrato e/ou período.</p>
          </header>

          <div className="grid grid-cols-1 gap-5 p-6 sm:grid-cols-2 lg:grid-cols-3">
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-600 dark:text-slate-400">Contrato</label>
              <GlassListbox
                value={contractId}
                onChange={(value) => setContractId(value ? Number(value) : null)}
                options={[
                  { value: null, label: 'Todos os contratos' },
                  ...contracts.map((contract) => ({
                    value: contract.contractId,
                    label: `${contract.contractor} • Nº ${contract.contractNumber ?? contract.contractId}`,
                  })),
                ]}
                placeholder="Todos os contratos"
              />
            </div>

            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-600 dark:text-slate-400">Data inicial</label>
              <AppDatePicker
                value={startDate}
                onChange={setStartDate}
                maxDate={endDate ?? undefined}
                disabled={contractId !== null}
              />
              {!startDate && submitted && <p className="text-xs text-red-500">Filtro obrigatório.</p>}
            </div>

            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-600 dark:text-slate-400">Data final</label>
              <AppDatePicker
                value={endDate}
                onChange={setEndDate}
                minDate={startDate ?? undefined}
                maxDate={new Date()}
                disabled={contractId !== null}
              />
              {!endDate && submitted && <p className="text-xs text-red-500">Filtro obrigatório.</p>}
            </div>
          </div>

          <footer className="flex flex-col justify-end gap-3 border-t border-slate-200 px-6 py-4 dark:border-white/10 sm:flex-row">
            <button
              type="button"
              onClick={resetFilters}
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
        <div className="mb-4 flex flex-col items-center gap-1 px-3 py-2">
          <h2 className="text-center text-base font-semibold text-slate-800 dark:text-slate-100 sm:text-lg">Relatório de saída e saldo por instalação</h2>
          {selectedContract && (
            <p className="text-center text-xs text-slate-500 dark:text-slate-400 sm:text-sm">
              Contrato nº <span className="font-medium text-slate-700 dark:text-slate-300">{selectedContract.contractNumber ?? selectedContract.contractId}</span>
            </p>
          )}
          <span className="mt-1 inline-flex items-center rounded-md bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600 dark:bg-slate-800 dark:text-slate-300">
            {startDate ? startDate.toLocaleDateString('pt-BR') : '-'} à {endDate ? endDate.toLocaleDateString('pt-BR') : '-'}
          </span>
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
