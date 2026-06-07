import {
  Dialog,
  DialogBackdrop,
  DialogPanel,
  DialogTitle,
  Transition,
  TransitionChild,
} from '@headlessui/react';
import { Fragment } from 'react';

interface ConfirmProps {
  open: boolean;
  title: string;
  description?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm: () => void;
  onClose: () => void;
  loading?: boolean;
  danger?: boolean;
}

export function Confirm({
  open,
  title,
  description,
  confirmLabel = 'Confirmar',
  cancelLabel = 'Cancelar',
  onConfirm,
  onClose,
  loading = false,
  danger = false,
}: ConfirmProps) {
  return (
    <Transition show={open} as={Fragment}>
      <Dialog onClose={onClose} className="relative z-[70]">
        <TransitionChild
          as={Fragment}
          enter="ease-out duration-200"
          enterFrom="opacity-0"
          enterTo="opacity-100"
          leave="ease-in duration-150"
          leaveFrom="opacity-100"
          leaveTo="opacity-0"
        >
          <DialogBackdrop className="fixed inset-0 bg-black/40 backdrop-blur-xs" />
        </TransitionChild>

        <div className="fixed inset-0 flex items-center justify-center p-4">
          <TransitionChild
            as={Fragment}
            enter="ease-out duration-200"
            enterFrom="opacity-0 translate-y-2 scale-95"
            enterTo="opacity-100 translate-y-0 scale-100"
            leave="ease-in duration-150"
            leaveFrom="opacity-100 translate-y-0 scale-100"
            leaveTo="opacity-0 translate-y-2 scale-95"
          >
            <DialogPanel className="w-full max-w-md rounded-2xl border border-white/10 bg-background p-5 text-slate-100 shadow-2xl backdrop-blur-2xl">
              <div className="mb-4 flex items-start justify-between gap-3 text-foreground">
                <DialogTitle className="text-base font-semibold">{title}</DialogTitle>
                <button
                  type="button"
                  onClick={onClose}
                  className="rounded-lg p-1.5 text-slate-400 transition hover:bg-white/10 hover:text-slate-200"
                  aria-label="Fechar"
                >
                  <i className="pi pi-times text-xs text-foreground" />
                </button>
              </div>

              {description && (
                <p className="mb-5 text-sm leading-relaxed text-foreground">{description}</p>
              )}

              <div className="flex items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={onClose}
                  className="rounded-2xl border border-zinc-200 px-4 py-2 text-sm font-semibold text-foreground transition hover:bg-zinc-300 dark:border-zinc-700 dark:hover:bg-zinc-700/50"
                >
                  {cancelLabel}
                </button>
                <button
                  type="button"
                  disabled={loading}
                  onClick={onConfirm}
                  className={[
                    'rounded-2xl px-4 py-2 text-sm font-semibold text-white transition disabled:opacity-50',
                    danger
                      ? 'bg-red-600 hover:bg-red-500'
                      : 'bg-gradient-to-r from-blue-600 to-cyan-500 hover:brightness-110',
                  ].join(' ')}
                >
                  {loading && <i className="pi pi-spin pi-spinner mr-1.5 text-xs" />}
                  {confirmLabel}
                </button>
              </div>
            </DialogPanel>
          </TransitionChild>
        </div>
      </Dialog>
    </Transition>
  );
}
