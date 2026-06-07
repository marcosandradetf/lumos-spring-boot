import { useEffect } from 'react';
import { useAppStore } from '@/store/use-app-store';

const APK_URL = 'https://api.thryon.com.br/minio/apk/com.thryon.apps.android.release_9_2.1.6.apk';
const APK_NAME = 'com.thryon.apps.android.release_9_2.1.6.apk';

export default function AppDownload() {
  const { setPageContext } = useAppStore();

  useEffect(() => {
    setPageContext(['Aplicativo', 'Download'], 'Instalar app no Android');
  }, [setPageContext]);

  const downloadApk = () => {
    const link = document.createElement('a');
    link.href = APK_URL;
    link.download = APK_NAME;
    link.click();
  };

  return (
    <section className="p-4 md:p-6">
      <div className="mx-auto max-w-6xl rounded-3xl border border-slate-200 bg-white p-6 shadow-sm dark:border-zinc-800 dark:bg-zinc-900 lg:p-10">
        <div className="grid gap-8 lg:grid-cols-[0.9fr_1.1fr] lg:gap-16">
          <article className="text-center lg:text-left">
            <img src="/ic_lumos.png" width="84" alt="Lumos App" className="mx-auto mb-6 lg:mx-0" />
            <h2 className="text-2xl font-semibold text-slate-900 dark:text-zinc-100">Celular Android</h2>

            <div className="mt-4 space-y-3 text-sm leading-7 text-slate-600 dark:text-zinc-300">
              <p>Para usar o app Lumos no seu celular Android, você precisa de um aparelho com Android 9.0 ou superior.</p>
              <p>Na primeira vez que você abre o app, é necessário estar conectado à internet para fazer login e carregar as informações iniciais.</p>
              <p>Os dados preenchidos ficam salvos no dispositivo e são enviados para o servidor quando a conexão retornar.</p>
              <p>O app irá solicitar permissões de localização e câmera para funcionamento da operação.</p>
            </div>
          </article>

          <article>
            <h1 className="text-3xl font-bold text-slate-900 dark:text-zinc-100">
              Instalar o Lumos no seu <span className="bg-gradient-to-r from-blue-600 to-cyan-500 bg-clip-text text-transparent">celular Android</span>
            </h1>

            <ol className="mt-6 space-y-3 text-base text-slate-700 dark:text-zinc-200">
              <li><i className="pi pi-download mr-2 text-blue-500" />Clique no botão <strong>Baixar APK</strong>.</li>
              <li><i className="pi pi-folder mr-2 text-blue-500" />Encontre o arquivo na pasta de downloads.</li>
              <li><i className="pi pi-check-circle mr-2 text-blue-500" />Abra o arquivo, conceda permissões e conclua a instalação.</li>
              <li><i className="pi pi-bolt mr-2 text-blue-500" />Abra o app Lumos e faça login.</li>
            </ol>

            <div className="mt-8">
              <button
                type="button"
                onClick={downloadApk}
                className="rounded-full bg-gradient-to-r from-blue-600 to-cyan-500 px-8 py-3 text-sm font-semibold text-white shadow-lg shadow-blue-500/25 hover:from-blue-500 hover:to-cyan-400"
              >
                <i className="pi pi-download mr-2" />Baixar APK
              </button>
            </div>
          </article>
        </div>
      </div>
    </section>
  );
}
