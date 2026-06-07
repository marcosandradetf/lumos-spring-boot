import { useState, useMemo, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { DataTable } from '@/shared/ui/data-table';
import type { Column } from '@/shared/ui/data-table';
import { GlassListbox } from '@/shared/components/glass-list-box';
import { useCatalogue } from '@/features/stock/hooks/use-catalogue';
import { useTypeSubtypes } from '@/features/stock/hooks/use-type-subtypes';
import type { MaterialFormDTO, MaterialTypeSubtype } from '@/features/stock/types/types';

export default function MaterialCatalog() {
  const navigate = useNavigate();
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();
  const [nameFilter, setNameFilter] = useState('');
  const [barcodeFilter, setBarcodeFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState<number | null>(null);

  useEffect(() => {
    setPageContext(['Estoque', 'Catálogo de Materiais'], 'Catálogo de Materiais');
  }, [setPageContext]);

  const { data: materials = [], isLoading } = useCatalogue();
  const { data: types = [] } = useTypeSubtypes();

  const filtered = useMemo(() => {
    return (materials as MaterialFormDTO[]).filter(m => {
      const matchName = !nameFilter || m.materialName.toLowerCase().includes(nameFilter.toLowerCase());
      const matchBarcode = !barcodeFilter || (m.barcode ?? '').includes(barcodeFilter);
      const matchType = typeFilter === null || m.materialType === typeFilter;
      return matchName && matchBarcode && matchType;
    });
  }, [materials, nameFilter, barcodeFilter, typeFilter]);

  const clearFilters = () => {
    setNameFilter('');
    setBarcodeFilter('');
    setTypeFilter(null);
  };

  const columns: Column<MaterialFormDTO>[] = [
    { key: 'barcode', header: 'Código de Barras', accessor: 'barcode' },
    { key: 'materialName', header: 'Nome', accessor: 'materialName', cellClassName: 'min-w-[200px]' },
    { key: 'buyUnit', header: 'Unid. Compra', accessor: 'buyUnit' },
    { key: 'requestUnit', header: 'Unid. Requisição', accessor: 'requestUnit' },
    {
      key: 'inactive',
      header: 'Status',
      render: (m) => (
        <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${m.inactive ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300' : 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300'}`}>
          {m.inactive ? 'Inativo' : 'Ativo'}
        </span>
      ),
    },
    {
      key: 'actions',
      header: '',
      cellClassName: 'w-20',
      render: (m) => (
        <div className="flex gap-1">
          <button
            type="button"
            onClick={() => m.materialId && navigate(`/estoque/cadastrar-material?materialId=${m.materialId}`)}
            className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-indigo-50 hover:text-indigo-600 dark:hover:bg-indigo-900/20 dark:hover:text-indigo-400 transition-colors"
            title="Editar"
          >
            <i className="pi pi-pencil text-xs" />
          </button>
          <button
            type="button"
            onClick={() => notify('Exclusão não disponível no catálogo.', 'info')}
            className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-900/20 dark:hover:text-red-400 transition-colors"
            title="Excluir"
          >
            <i className="pi pi-trash text-xs" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <section className="p-4 md:p-6 space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold text-slate-800 dark:text-zinc-100">
          Catálogo de Materiais
        </h1>
        <button
          type="button"
          onClick={() => navigate('/estoque/cadastrar-material')}
          className="flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 transition-colors"
        >
          <i className="pi pi-plus text-sm" />
          Cadastrar Material
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3">
        <input
          type="text"
          placeholder="Filtrar por nome"
          value={nameFilter}
          onChange={(e) => setNameFilter(e.target.value)}
          className="rounded-xl border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-3 py-2 text-sm text-slate-800 dark:text-zinc-100 placeholder:text-slate-400 outline-none focus:border-indigo-400 transition-colors"
        />
        <input
          type="text"
          placeholder="Filtrar por código de barras"
          value={barcodeFilter}
          onChange={(e) => setBarcodeFilter(e.target.value)}
          className="rounded-xl border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-3 py-2 text-sm text-slate-800 dark:text-zinc-100 placeholder:text-slate-400 outline-none focus:border-indigo-400 transition-colors"
        />
        <GlassListbox
          value={typeFilter}
          onChange={(value) => setTypeFilter(value === null ? null : Number(value))}
          placeholder="Todos os tipos"
          options={[
            { value: null, label: 'Todos os tipos' },
            ...(types as MaterialTypeSubtype[]).map((type) => ({
              value: type.typeId,
              label: type.typeName,
            })),
          ]}
        />
        <button
          type="button"
          onClick={clearFilters}
          className="flex items-center justify-center gap-2 rounded-xl border border-slate-200 dark:border-zinc-700 px-3 py-2 text-sm text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors"
        >
          <i className="pi pi-filter-slash text-sm" />
          Limpar filtros
        </button>
      </div>

      <DataTable
        columns={columns}
        data={filtered}
        rowKey={(m) => m.materialId ?? m.materialName}
        loading={isLoading}
        emptyMessage="Nenhum material encontrado no catálogo."
        pageSize={15}
      />
    </section>
  );
}
