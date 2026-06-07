interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export function Pagination({ currentPage, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  const pages = buildPageRange(currentPage, totalPages);

  return (
    <div className="flex items-center justify-end gap-1 px-1 py-3">
      <PageBtn
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 0}
        aria-label="Página anterior"
      >
        <i className="pi pi-chevron-left text-xs" />
      </PageBtn>

      {pages.map((page, i) =>
        page === '...' ? (
          <span key={`ellipsis-${i}`} className="px-2 text-sm text-slate-400 dark:text-zinc-500">…</span>
        ) : (
          <PageBtn
            key={page}
            onClick={() => onPageChange(page as number)}
            active={page === currentPage}
          >
            {(page as number) + 1}
          </PageBtn>
        ),
      )}

      <PageBtn
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage >= totalPages - 1}
        aria-label="Próxima página"
      >
        <i className="pi pi-chevron-right text-xs" />
      </PageBtn>
    </div>
  );
}

function buildPageRange(current: number, total: number): (number | '...')[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i);

  const result: (number | '...')[] = [];
  const add = (n: number) => { if (!result.includes(n)) result.push(n); };

  add(0);
  if (current > 2) result.push('...');
  for (let p = Math.max(1, current - 1); p <= Math.min(total - 2, current + 1); p++) add(p);
  if (current < total - 3) result.push('...');
  add(total - 1);

  return result;
}

interface PageBtnProps {
  onClick: () => void;
  disabled?: boolean;
  active?: boolean;
  children: React.ReactNode;
  'aria-label'?: string;
}

function PageBtn({ onClick, disabled, active, children, 'aria-label': ariaLabel }: PageBtnProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={ariaLabel}
      className={[
        'flex h-8 min-w-[2rem] items-center justify-center rounded-lg px-2 text-sm font-medium transition-colors',
        active
          ? 'bg-indigo-600 text-white shadow-sm'
          : disabled
            ? 'cursor-not-allowed text-slate-300 dark:text-zinc-600'
            : 'text-slate-600 dark:text-zinc-400 hover:bg-slate-100 dark:hover:bg-zinc-800 hover:text-slate-900 dark:hover:text-zinc-100',
      ].join(' ')}
    >
      {children}
    </button>
  );
}
