import { useEffect } from 'react';
import { useParams } from 'react-router-dom';

import { useAppStore } from '@/store/use-app-store';

export default function AccessDenied() {
  const { section } = useParams();
  const { setPageContext } = useAppStore();

  useEffect(() => {
    const sectionLabel = section ? section.replaceAll('-', ' ') : 'área';
    setPageContext(['Acesso negado'], `Acesso negado • ${sectionLabel}`);
  }, [section, setPageContext]);

  return (
    <section className="flex min-h-[calc(100vh-52px)] items-center justify-center px-4 py-8">
      <article className="w-full max-w-2xl rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-sm dark:border-zinc-800 dark:bg-zinc-900">
        <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-100 text-rose-600 dark:bg-rose-500/20 dark:text-rose-300">
          <i className="pi pi-lock text-2xl" />
        </div>
        <h1 className="text-2xl font-semibold text-slate-900 dark:text-zinc-100">Acesso Negado</h1>
        <p className="mt-3 text-sm leading-7 text-slate-600 dark:text-zinc-300">
          Sua função atual no sistema não permite acessar esta área. Fale com o administrador caso precise de acesso
          ou use o menu para acessar outra rotina.
        </p>
      </article>
    </section>
  );
}
