import { useState } from 'react';
import {useNotify} from '../hooks/use-notify';

interface EmbeddedDocPanelProps {
  open: boolean;
  title: string;
  description?: string;
  url: string;
  onClose: () => void;
  showShareButton?: boolean;
}

export function EmbeddedDocPanel({
  open,
  title,
  description,
  url,
  onClose,
  showShareButton = true,
}: EmbeddedDocPanelProps) {
  const [shareModalOpen, setShareModalOpen] = useState(false);
  const notify = useNotify();

  if (!open) {
    return null;
  }

  const shareMessage = `${title}\n\n${description ?? 'Documentação Lumos'}\n${url}`;

  const isDesktop = (() => {
    if (typeof navigator === 'undefined') return true;
    const ua = navigator.userAgent;
    return /Windows|Macintosh|Linux/i.test(ua);
  })();

  const copyMessage = async () => {
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(shareMessage);
      } else {
        const textArea = document.createElement('textarea');
        textArea.value = shareMessage;
        textArea.style.position = 'fixed';
        textArea.style.opacity = '0';
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
      }
      setShareModalOpen(false);
      notify.success('Mensagem copiada para a área de transferência.');
    } catch {
      notify.error('Não foi possível copiar a mensagem.');
    } finally {
      setShareModalOpen(false);
    }
  };

  const shareWhatsApp = () => {
    window.open(`https://wa.me/?text=${encodeURIComponent(shareMessage)}`, '_blank', 'noopener,noreferrer');
    setShareModalOpen(false);
  };

  const shareEmail = () => {
    const subject = encodeURIComponent(`${title} • Lumos IP™`);
    const body = encodeURIComponent(shareMessage);
    window.open(`mailto:?subject=${subject}&body=${body}`, '_blank');
    setShareModalOpen(false);
  };

  const handleShare = async () => {
    if (!isDesktop && typeof navigator !== 'undefined' && navigator.share) {
      try {
        await navigator.share({
          title: `${title} • Lumos IP™`,
          text: description ?? 'Documentação Lumos',
          url,
        });
        return;
      } catch {
        // fallback para modal custom
      }
    }

    setShareModalOpen(true);
  };

  return (
    <>
      <div
        className="fixed inset-0 z-[90] bg-slate-950/35 backdrop-blur-[2px]"
        onClick={onClose}
      />

      <section className="fixed inset-y-0 right-0 z-[95] flex h-full w-full max-w-[96vw] flex-col bg-white shadow-2xl dark:bg-slate-900 lg:w-[min(1200px,92vw)]">
        <div className="flex flex-col gap-3 border-b border-slate-200 px-4 py-4 dark:border-slate-800 sm:flex-row sm:items-center sm:justify-between sm:px-5">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500 dark:text-slate-400">
              Documentação incorporada
            </p>
            <h3 className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
              {title}
            </h3>
            {description ? (
              <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                {description}
              </p>
            ) : null}
          </div>

          <div className="flex w-full justify-center gap-2 sm:w-auto">
            {showShareButton ? (
              <button
                type="button"
                onClick={() => void handleShare()}
                className="w-full rounded-full border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 dark:border-slate-700 dark:text-slate-100 dark:hover:bg-slate-800 sm:w-auto"
              >
                <i className="pi pi-share-alt mr-1 text-xs" />
                Compartilhar
              </button>
            ) : null}

            <button
              type="button"
              onClick={onClose}
              className="w-full rounded-full border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 dark:border-slate-700 dark:text-slate-100 dark:hover:bg-slate-800 sm:w-auto"
            >
              Fechar
            </button>
          </div>
        </div>

        <div className="flex-1 p-4 sm:p-5">
          <iframe
            src={url}
            className="min-h-[85vh] w-full rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-950"
            referrerPolicy="strict-origin-when-cross-origin"
            title={title}
          />
        </div>
      </section>

      {shareModalOpen ? (
        <div
          className="fixed inset-0 z-[120] flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              setShareModalOpen(false);
            }
          }}
        >
          <div className="w-full max-w-md overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl dark:border-zinc-800 dark:bg-zinc-900">
            <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4 dark:border-zinc-800">
              <h3 className="text-base font-semibold text-slate-800 dark:text-zinc-100">Compartilhar</h3>
              <button
                type="button"
                onClick={() => setShareModalOpen(false)}
                className="flex h-7 w-7 items-center justify-center rounded-full text-slate-400 hover:bg-slate-100 hover:text-slate-700 dark:hover:bg-zinc-800 dark:hover:text-zinc-200"
              >
                <i className="pi pi-times text-sm" />
              </button>
            </div>
            <div className="px-5 py-4">
              <p className="text-sm text-slate-600 dark:text-zinc-300">
                Escolha como deseja compartilhar.
              </p>
            </div>
            <div className="flex items-center justify-end gap-2 border-t border-slate-200 px-5 py-3 dark:border-zinc-800">
              <button
                type="button"
                onClick={() => void copyMessage()}
                className="rounded-full border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-800"
              >
                <i className="pi pi-copy mr-1 text-xs" />
                Copiar mensagem
              </button>
              <button
                type="button"
                onClick={shareWhatsApp}
                className="rounded-full bg-green-600 px-3 py-2 text-sm font-medium text-white hover:bg-green-500"
              >
                <i className="pi pi-whatsapp mr-1 text-xs" />
                WhatsApp
              </button>
              <button
                type="button"
                onClick={shareEmail}
                className="rounded-full bg-slate-700 px-3 py-2 text-sm font-medium text-white hover:bg-slate-600 dark:bg-zinc-700 dark:hover:bg-zinc-600"
              >
                <i className="pi pi-envelope mr-1 text-xs" />
                E-mail
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
