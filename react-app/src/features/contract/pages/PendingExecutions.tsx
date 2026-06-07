import { useEffect } from 'react';
import { useAppStore } from '@/store/use-app-store';
import { DataTable } from '@/shared/ui/data-table';
import type { Column } from '@/shared/ui/data-table';
import { useInstallationsWaitingValidation } from '@/features/executions/hooks/useInstallationsWaitingValidation';

interface PendingInstallation {
  installation_id: number;
  description: string;
  contractor: string;
  team: string;
  finished_at: string;
  type: string;
}

const fmtDate = (s: string) => s ? new Date(s).toLocaleDateString('pt-BR') : '—';

export default function PendingExecutions() {
  const { setPageContext } = useAppStore();

  useEffect(() => {
    setPageContext(['Contratos', 'Vincular Instalações'], 'Vincular Instalações');
  }, [setPageContext]);

  const { data: installations = [], isLoading } = useInstallationsWaitingValidation();

  const columns: Column<PendingInstallation>[] = [
    { key: 'installation_id', header: '#', accessor: 'installation_id', cellClassName: 'w-16 text-slate-400 dark:text-zinc-500' },
    { key: 'description', header: 'Descrição', accessor: 'description' },
    { key: 'contractor', header: 'Contratante', accessor: 'contractor' },
    { key: 'team', header: 'Equipe', accessor: 'team' },
    { key: 'type', header: 'Tipo', render: (r) => (
      <span className="inline-flex rounded-full bg-slate-100 dark:bg-zinc-800 px-2.5 py-1 text-xs text-slate-600 dark:text-zinc-300">{r.type}</span>
    )},
    { key: 'finished_at', header: 'Concluída em', render: (r) => fmtDate(r.finished_at) },
  ];

  return (
    <section className="p-4 md:p-6 space-y-4">
      <h1 className="text-2xl font-semibold text-slate-800 dark:text-zinc-100">Vincular Instalações</h1>
      <p className="text-sm text-slate-500 dark:text-zinc-400">
        Instalações concluídas sem vínculo contratual. Selecione uma para associar aos itens contratuais.
      </p>
      <DataTable
        columns={columns}
        data={installations as PendingInstallation[]}
        rowKey={r => r.installation_id}
        loading={isLoading}
        emptyMessage="Nenhuma instalação pendente de vínculo contratual."
      />
    </section>
  );
}
