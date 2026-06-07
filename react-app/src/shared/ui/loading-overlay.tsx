interface LoadingOverlayProps {
  loading: boolean;
}

export function LoadingOverlay({ loading }: LoadingOverlayProps) {
  if (!loading) return null;

  return (
    <div className="absolute inset-0 z-10 flex items-center justify-center rounded-inherit bg-white/80 dark:bg-zinc-900/90">
      <i className="pi pi-spin pi-spinner text-2xl text-indigo-500" />
    </div>
  );
}
