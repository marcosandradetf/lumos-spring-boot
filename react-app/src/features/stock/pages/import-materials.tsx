import { useEffect } from 'react';
import { Link } from 'react-router-dom';

import { useAppStore } from '@/store/use-app-store';

export default function ImportMaterials() {
  const { setPageContext } = useAppStore();

  useEffect(() => {
    setPageContext(['Estoque', 'Importar planilha'], 'Importar Materiais (.xlsx)');
  }, [setPageContext]);

  return (
    <section className="p-4 md:p-6">
      <article className="mx-auto max-w-3xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-zinc-800 dark:bg-zinc-900">
        <h1 className="text-2xl font-semibold text-slate-900 dark:text-zinc-100">Importar Materiais (.xlsx)</h1>
        <p className="mt-3 text-sm leading-7 text-slate-600 dark:text-zinc-300">
          Esta tela foi migrada para manter compatibilidade de rota do Angular. O fluxo completo de importação por planilha
          será refinado no próximo ciclo.
        </p>

        <div className="mt-6 rounded-xl bg-blue-50 p-4 text-sm text-blue-800 dark:bg-blue-500/10 dark:text-blue-200">
          Enquanto isso, você pode realizar cadastro individual em <strong>Cadastro de Materiais</strong>.
        </div>

        <div className="mt-6 flex flex-wrap gap-3">
          <Link
            to="/estoque/cadastrar-material"
            className="rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-4 py-2 text-sm font-semibold text-white"
          >
            <i className="pi pi-plus-circle mr-2" />Ir para cadastro
          </Link>
          <Link
            to="/estoque/catalogo-materiais"
            className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 dark:border-zinc-700 dark:text-zinc-200"
          >
            <i className="pi pi-table mr-2" />Ver catálogo
          </Link>
        </div>
      </article>
    </section>
  );
}
