import { Modal, ModalHeader } from '../ui/modal';

interface PdfPreviewModalProps {
  open: boolean;
  url: string | null;
  title?: string;
  onClose: () => void;
}

export function PdfPreviewModal({
  open,
  url,
  title = 'Visualizar PDF',
  onClose,
}: PdfPreviewModalProps) {
  return (
    <Modal open={open} onClose={onClose} className="w-[96vw] max-w-6xl">
      <ModalHeader title={title} onClose={onClose} />
      <div className="h-[80vh] bg-slate-100 p-3 dark:bg-zinc-950">
        {url ? (
          <iframe
            src={url}
            title={title}
            className="h-full w-full rounded-2xl border border-slate-200 bg-white dark:border-zinc-800"
          />
        ) : (
          <div className="flex h-full items-center justify-center text-sm text-slate-500 dark:text-zinc-400">
            Nenhum arquivo disponível.
          </div>
        )}
      </div>
    </Modal>
  );
}
