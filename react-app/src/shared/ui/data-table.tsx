import type { ReactNode } from 'react';
import { useState } from 'react';
import { Pagination } from './pagination';
import { SkeletonTable } from './skeleton-table';

export interface Column<T> {
  key: string;
  header: string;
  render?: (row: T) => ReactNode;
  accessor?: keyof T;
  headerClassName?: string;
  cellClassName?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  rowKey: (row: T) => string | number;
  loading?: boolean;
  onSearch?: (value: string) => void;
  searchPlaceholder?: string;
  emptyMessage?: string;
  onRowClick?: (row: T) => void;
  className?: string;
  pageSize?: number;
  serverPagination?: {
    currentPage: number;
    totalPages: number;
    onPageChange: (page: number) => void;
  };
}

function DataTable<T>({
  columns,
  data,
  rowKey,
  loading = false,
  onSearch,
  searchPlaceholder = 'Pesquisar...',
  emptyMessage = 'Nenhum registro encontrado.',
  onRowClick,
  className = '',
  pageSize,
  serverPagination,
}: DataTableProps<T>) {
  const [localPage, setLocalPage] = useState(0);

  const effectivePage = serverPagination ? serverPagination.currentPage : localPage;
  const rows = pageSize && !serverPagination
    ? data.slice(effectivePage * pageSize, effectivePage * pageSize + pageSize)
    : data;
  const totalPages = pageSize && !serverPagination ? Math.ceil(data.length / pageSize) : 0;

  if (loading) return <SkeletonTable columns={columns.length} />;

  return (
    <div className={`rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 overflow-hidden ${className}`}>
      {onSearch && (
        <div className="px-4 py-3 border-b border-slate-100 dark:border-zinc-800">
          <div className="relative max-w-xs">
            <i className="pi pi-search absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm" />
            <input
              type="text"
              placeholder={searchPlaceholder}
              onChange={(e) => onSearch(e.target.value)}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 pl-9 pr-3 py-2 text-sm text-slate-800 dark:text-zinc-100 placeholder:text-slate-400 outline-none focus:border-indigo-400 transition-colors"
            />
          </div>
        </div>
      )}

      <div className="overflow-x-auto">
        <table className="min-w-full">
          <thead className="bg-slate-50 dark:bg-zinc-900/50 text-left">
            <tr>
              {columns.map(col => (
                <th
                  key={col.key}
                  className={`px-4 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-zinc-400 ${col.headerClassName ?? ''}`}
                >
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>

          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-4 py-10 text-center text-sm text-slate-500 dark:text-zinc-400">
                  <i className="pi pi-inbox text-3xl block opacity-40" />
                  <p>{emptyMessage}</p>
                </td>
              </tr>
            ) : (
              rows.map(row => (
                <tr
                  key={rowKey(row)}
                  onClick={onRowClick ? () => onRowClick(row) : undefined}
                  className={`border-t border-slate-100 dark:border-zinc-800 ${onRowClick ? 'cursor-pointer hover:bg-slate-50 dark:hover:bg-zinc-800/50' : ''} transition-colors`}
                >
                  {columns.map(col => (
                    <td key={col.key} className={`px-4 py-3 text-sm text-slate-700 dark:text-zinc-200 ${col.cellClassName ?? ''}`}>
                      {col.render
                        ? col.render(row)
                        : col.accessor
                          ? String(row[col.accessor] ?? '')
                          : ''}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {serverPagination && (
        <div className="border-t border-slate-100 dark:border-zinc-800 px-2">
          <Pagination
            currentPage={serverPagination.currentPage}
            totalPages={serverPagination.totalPages}
            onPageChange={serverPagination.onPageChange}
          />
        </div>
      )}

      {pageSize && !serverPagination && totalPages > 1 && (
        <div className="border-t border-slate-100 dark:border-zinc-800 px-2">
          <Pagination
            currentPage={localPage}
            totalPages={totalPages}
            onPageChange={setLocalPage}
          />
        </div>
      )}
    </div>
  );
}

export { DataTable };
