interface SkeletonTableProps {
  columns: number;
  rows?: number;
}

export function SkeletonTable({ columns, rows = 8 }: SkeletonTableProps) {
  return (
    <div className="overflow-x-auto rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900">
      <table className="min-w-full">
        <thead className="bg-slate-50 dark:bg-zinc-900/50">
          <tr>
            {Array.from({ length: columns }).map((_, i) => (
              <th key={i} className="px-4 py-3">
                <div className="h-3.5 w-24 rounded-full bg-slate-200 dark:bg-zinc-700 animate-pulse" />
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {Array.from({ length: rows }).map((_, rowIdx) => (
            <tr key={rowIdx} className="border-t border-slate-100 dark:border-zinc-800">
              {Array.from({ length: columns }).map((_, colIdx) => (
                <td key={colIdx} className="px-4 py-3">
                  <div
                    className="h-3.5 rounded-full bg-slate-100 dark:bg-zinc-800 animate-pulse"
                    style={{ width: `${60 + ((rowIdx * columns + colIdx) % 5) * 8}%` }}
                  />
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
