import { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';

import { useAppStore } from '@/store/use-app-store';

export default function ValidateExecution() {
  const { id } = useParams();
  const { setPageContext } = useAppStore();

  useEffect(() => {
    setPageContext(['Contratos', 'Validar execução'], 'Validar Execução');
  }, [setPageContext]);

  return (
    <section className="p-4 md:p-6">
      <article className="mx-auto max-w-3xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-zinc-800 dark:bg-zinc-900">
        <h1 className="text-2xl font-semibold text-slate-900 dark:text-zinc-100">Validação de Execução</h1>
        <p className="mt-2 text-sm text-slate-500 dark:text-zinc-400">Execução: {id ?? '-'}</p>

        <p className="mt-4 text-sm leading-7 text-slate-600 dark:text-zinc-300">
          Esta rota foi migrada para manter o fluxo de navegação do Angular. A tela detalhada de validação será
          incorporada no próximo ciclo junto da regra completa de serviços não executados.
        </p>

        <div className="mt-6 flex flex-wrap gap-3">
          <Link
            to="/contratos/instalacoes-pendentes"
            className="rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-4 py-2 text-sm font-semibold text-white"
          >
            <i className="pi pi-arrow-left mr-2" />Voltar para Instalações Pendentes
          </Link>
          <Link
            to="/relatorios/gerenciamento"
            className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 dark:border-zinc-700 dark:text-zinc-200"
          >
            <i className="pi pi-file-pdf mr-2" />Ir para Relatórios Agrupados
          </Link>
        </div>
      </article>
    </section>
  );
}
