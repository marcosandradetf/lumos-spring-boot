import { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';

import { useAppStore } from '@/store/use-app-store';

export default function ImportPreMeasurements() {
  const { id } = useParams();
  const { setPageContext } = useAppStore();

  useEffect(() => {
    setPageContext(['Pré-medição', 'Importar por contrato'], 'Importar Pré-medições');
  }, [setPageContext]);

  return (
    <section className="p-4 md:p-6">
      <article className="mx-auto max-w-3xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-zinc-800 dark:bg-zinc-900">
        <h1 className="text-2xl font-semibold text-slate-900 dark:text-zinc-100">Importar Pré-medições</h1>
        <p className="mt-2 text-sm text-slate-500 dark:text-zinc-400">Contrato selecionado: {id ?? '-'}</p>

        <p className="mt-4 text-sm leading-7 text-slate-600 dark:text-zinc-300">
          Esta rota foi criada para preservar o fluxo do Angular enquanto finalizamos o componente de importação em React.
        </p>

        <div className="mt-6 rounded-xl bg-amber-50 p-4 text-sm text-amber-800 dark:bg-amber-500/10 dark:text-amber-200">
          O usuário pode continuar o fluxo pela listagem de pré-medições sem perda de contexto.
        </div>

        <div className="mt-6">
          <Link
            to="/pre-medicao/pendente"
            className="inline-flex rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-4 py-2 text-sm font-semibold text-white"
          >
            <i className="pi pi-arrow-right mr-2" />Ir para Pré-medições
          </Link>
        </div>
      </article>
    </section>
  );
}
