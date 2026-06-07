import { useEffect } from 'react';

import { useAppStore } from '@/store/use-app-store';

interface ServiceRequestMapProps {
  mode?: 'manual' | 'round';
}

export default function ServiceRequestMap({ mode = 'manual' }: ServiceRequestMapProps) {
  const { setPageContext } = useAppStore();

  useEffect(() => {
    const title = mode === 'round' ? 'Modo Ronda' : 'Abrir Chamado';
    setPageContext(['Ordens de Serviço', title], title);
  }, [mode, setPageContext]);

  return (
    <section className="p-4 md:p-6">
      <article className="mx-auto max-w-3xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-zinc-800 dark:bg-zinc-900">
        <h1 className="text-2xl font-semibold text-slate-900 dark:text-zinc-100">
          {mode === 'round' ? 'Modo Ronda' : 'Abrir Chamado'}
        </h1>
        <p className="mt-2 text-sm text-slate-500 dark:text-zinc-400">
          Esta tela está em transição para o novo módulo e será concluída no próximo ciclo.
        </p>
      </article>
    </section>
  );
}
