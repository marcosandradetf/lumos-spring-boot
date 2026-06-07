import { useState, useEffect, useCallback } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { Pagination } from '@/shared/ui/pagination';
import { SkeletonTable } from '@/shared/ui/skeleton-table';
import { Modal, ModalHeader, ModalBody, ModalFooter } from '@/shared/ui/modal';
import { LoadingOverlay } from '@/shared/ui/loading-overlay';
import { GlassListbox } from '@/shared/components/glass-list-box';
import { StockMovementStepper } from '@/features/stock/components/stock-movement-stepper';
import { materialApi } from '@/features/stock/api/material-api';
import { stockApi } from '@/features/stock/api/stock-api';
import { stockKeys } from '@/features/stock/api/query-keys';
import { useDeposits } from '@/features/stock/hooks/use-deposits';
import type { Deposit, MaterialStockResponse, StockMovementDTO } from '@/features/stock/types/types';

function calcTotal(item: StockMovementDTO): string {
  if (item.requestUnit !== item.buyUnit) {
    const qty = parseFloat(item.inputQuantity) || 0;
    const pkg = parseFloat(item.quantityPackage) || 0;
    return (qty * pkg).toString();
  }
  return item.inputQuantity;
}

export default function StockMovement() {
  const navigate = useNavigate();
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();

  const [deposit, setDeposit] = useState<Deposit | null>(null);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [cart, setCart] = useState<StockMovementDTO[]>([]);
  const [cartOpen, setCartOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  useEffect(() => {
    setPageContext(['Estoque', 'Movimentar Estoque'], 'Movimentar Estoque');
  }, [setPageContext]);

  const { data: deposits = [] } = useDeposits();
  const createMovementMutation = useMutation({
    mutationFn: (cart: StockMovementDTO[]) => stockApi.createMovement(cart),
  });

  const hasTruck = (deposits as Deposit[]).some(d => d.isTruck);
  const hasFixed = (deposits as Deposit[]).some(d => !d.isTruck);

  const { data: materialsPage, isLoading: matLoading } = useQuery({
    queryKey: stockKeys.materialsBrowse(deposit?.idDeposit, page, search),
    queryFn: () => {
      if (!deposit?.idDeposit) {
        return null;
      }

      return search
        ? materialApi.getBySearch(page, 15, deposit.idDeposit, search)
        : materialApi.getMaterials(page, 15, deposit.idDeposit);
    },
    enabled: !!deposit?.idDeposit,
  });

  const materials: MaterialStockResponse[] = (materialsPage as { content?: MaterialStockResponse[]; data?: MaterialStockResponse[] } | null)?.content
    ?? (materialsPage as { content?: MaterialStockResponse[]; data?: MaterialStockResponse[] } | null)?.data
    ?? [];

  const totalPages = (materialsPage as { totalPages?: number; last?: boolean } | null)?.totalPages ?? 0;

  const isInCart = (id: number) => cart.some(c => c.materialStockId === id);

  const toggleItem = useCallback((item: MaterialStockResponse) => {
    setCart(prev => {
      const exists = prev.find(c => c.materialStockId === item.materialStockId);
      if (exists) {
        return prev.filter(c => c.materialStockId !== item.materialStockId);
      }
      return [...prev, {
        materialStockId: item.materialStockId,
        materialName: item.materialName,
        barcode: item.barcode,
        description: item.materialName,
        buyUnit: item.buyUnit,
        requestUnit: item.requestUnit,
        inputQuantity: '',
        priceTotal: '',
        quantityPackage: '',
        totalQuantity: '',
        hidden: false,
        invalid: false,
      }];
    });
  }, []);

  const updateCartItem = (materialStockId: number, key: keyof StockMovementDTO, value: string) => {
    setCart(prev => prev.map(item => {
      if (item.materialStockId !== materialStockId) return item;
      const updated = { ...item, [key]: value };
      updated.totalQuantity = calcTotal(updated);
      return updated;
    }));
  };

  const validateCart = (): boolean => {
    for (const item of cart) {
      if (!item.inputQuantity || parseFloat(item.inputQuantity) <= 0) {
        notify(`Informe a quantidade para: ${item.materialName}`, 'warn');
        return false;
      }
      if (item.requestUnit !== item.buyUnit && !item.quantityPackage) {
        notify(`Informe a qtde. por embalagem para: ${item.materialName}`, 'warn');
        return false;
      }
    }
    return true;
  };

  const handleSubmit = async () => {
    if (!validateCart()) return;
    setLoading(true);
    try {
      await createMovementMutation.mutateAsync(cart);
      setCart([]);
      setCartOpen(false);
      setSubmitted(true);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Erro ao salvar movimentação.';
      notify(msg, 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleSelectDeposit = (depositId: string) => {
    const found = (deposits as Deposit[]).find(d => d.idDeposit === Number(depositId));
    setDeposit(found ?? null);
    setPage(0);
    setCart([]);
  };

  if (submitted) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center p-6">
        <div className="w-full max-w-md rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-8 text-center shadow-sm">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-900/30">
            <i className="pi pi-check text-2xl text-emerald-600 dark:text-emerald-400" />
          </div>
          <h2 className="text-xl font-semibold text-slate-800 dark:text-zinc-100">Movimentação criada!</h2>
          <p className="mt-2 text-sm text-slate-500 dark:text-zinc-400">
            Aguardando aprovação do responsável.
          </p>
          <div className="mt-6 flex flex-col gap-2">
            <button type="button" onClick={() => setSubmitted(false)}
              className="rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-500 transition-colors">
              Nova movimentação
            </button>
            <button type="button" onClick={() => navigate('/estoque/movimentar-estoque-pendente')}
              className="rounded-xl border border-slate-200 dark:border-zinc-700 px-4 py-2.5 text-sm font-semibold text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors">
              Ver pendentes
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (!hasTruck || !hasFixed) {
    return (
      <section className="p-4 md:p-6 space-y-4">
        <StockMovementStepper />
        <div className="rounded-2xl border border-amber-200 dark:border-amber-900/40 bg-amber-50 dark:bg-amber-950/20 p-6 space-y-3">
          <h2 className="text-base font-semibold text-amber-800 dark:text-amber-200">
            Configure o sistema antes de movimentar estoque
          </h2>
          <ul className="list-disc pl-5 text-sm text-amber-700 dark:text-amber-300 space-y-1">
            {!hasFixed && <li>Cadastre ao menos um <strong>almoxarifado fixo</strong>.</li>}
            {!hasTruck && <li>Cadastre ao menos um <strong>caminhão (depósito móvel)</strong>.</li>}
          </ul>
          <button type="button" onClick={() => navigate('/estoque/almoxarifados')}
            className="inline-flex items-center gap-2 rounded-xl bg-amber-600 px-4 py-2 text-sm font-semibold text-white hover:bg-amber-500 transition-colors">
            <i className="pi pi-home" /> Gerenciar Almoxarifados
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="p-4 md:p-6 space-y-4">
      <StockMovementStepper />

      {/* Deposit selector */}
      {!deposit && (
        <div className="rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-4">
          <label className="block text-sm font-medium text-slate-700 dark:text-zinc-200 mb-2">
            Selecione o almoxarifado ou caminhão
          </label>
          <GlassListbox
            value={null}
            onChange={(value) => handleSelectDeposit(value === null ? '' : String(value))}
            placeholder="Escolha um almoxarifado"
            options={(deposits as Deposit[]).map((currentDeposit) => ({
              value: currentDeposit.idDeposit,
              label: currentDeposit.isTruck
                ? `CAMINHÃO ${currentDeposit.teamName?.toUpperCase() ?? ''}`
                : currentDeposit.depositName,
            }))}
          />
        </div>
      )}

      {deposit && (
        <>
          {/* Header with deposit name + change button */}
          <div className="flex items-center justify-between">
            <div>
              <span className="text-xs text-slate-500 dark:text-zinc-400">Almoxarifado selecionado</span>
              <h2 className="text-base font-semibold text-slate-800 dark:text-zinc-100">
                {deposit.isTruck ? `CAMINHÃO ${deposit.teamName?.toUpperCase() ?? ''}` : deposit.depositName}
              </h2>
            </div>
            <button type="button" onClick={() => { setDeposit(null); setCart([]); }}
              className="text-sm text-indigo-600 dark:text-indigo-400 hover:underline">
              Alterar
            </button>
          </div>

          {/* Search */}
          <div className="relative max-w-xs">
            <i className="pi pi-search absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm" />
            <input type="text" placeholder="Buscar material..." value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); }}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 pl-9 pr-3 py-2 text-sm text-slate-800 dark:text-zinc-100 outline-none focus:border-indigo-400 transition-colors"
            />
          </div>

          {/* Materials table */}
          {matLoading ? (
            <SkeletonTable columns={6} rows={6} />
          ) : (
            <div className="rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 overflow-hidden">
              <div className="overflow-x-auto">
                <table className="min-w-full">
                  <thead className="bg-slate-50 dark:bg-zinc-900/50 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-zinc-400">
                    <tr>
                      <th className="px-4 py-3 w-10"></th>
                      <th className="px-4 py-3">Código</th>
                      <th className="px-4 py-3 min-w-[200px]">Material</th>
                      <th className="px-4 py-3">UN. Compra</th>
                      <th className="px-4 py-3">UN. Req.</th>
                      <th className="px-4 py-3">Em Estoque</th>
                    </tr>
                  </thead>
                  <tbody>
                    {materials.length === 0 ? (
                      <tr><td colSpan={6} className="px-4 py-10 text-center text-sm text-slate-400 dark:text-zinc-500">
                        <i className="pi pi-inbox text-3xl block mb-2 opacity-40" />
                        Nenhum material encontrado neste almoxarifado.
                      </td></tr>
                    ) : materials.map(m => {
                      const inCart = isInCart(m.materialStockId);
                      return (
                        <tr key={m.materialStockId} onClick={() => toggleItem(m)}
                          className={`border-t border-slate-100 dark:border-zinc-800 cursor-pointer transition-colors ${inCart ? 'bg-indigo-50 dark:bg-indigo-900/20' : 'hover:bg-slate-50 dark:hover:bg-zinc-800/50'}`}>
                          <td className="px-4 py-3">
                            <div className={`w-5 h-5 rounded-md border-2 flex items-center justify-center transition-colors ${inCart ? 'bg-indigo-600 border-indigo-600' : 'border-slate-300 dark:border-zinc-600'}`}>
                              {inCart && <i className="pi pi-check text-white text-[10px]" />}
                            </div>
                          </td>
                          <td className="px-4 py-3 text-xs text-slate-500 dark:text-zinc-400">{m.barcode ?? '—'}</td>
                          <td className="px-4 py-3 text-sm text-slate-700 dark:text-zinc-200">{m.materialName}</td>
                          <td className="px-4 py-3 text-sm text-slate-500 dark:text-zinc-400">{m.buyUnit}</td>
                          <td className="px-4 py-3 text-sm text-slate-500 dark:text-zinc-400">{m.requestUnit}</td>
                          <td className="px-4 py-3 text-sm font-medium">{m.stockQuantity}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
              <div className="border-t border-slate-100 dark:border-zinc-800 px-2">
                <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
              </div>
            </div>
          )}

          {/* Sticky footer */}
          {cart.length > 0 && (
            <div className="sticky bottom-3 z-10 flex justify-end">
              <button type="button" onClick={() => setCartOpen(true)}
                className="flex items-center gap-2 rounded-xl bg-indigo-600 px-5 py-3 text-sm font-semibold text-white shadow-lg hover:bg-indigo-500 transition-colors">
                <i className="pi pi-shopping-cart" />
                Revisar movimentação
                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-white text-indigo-700 text-xs font-bold">{cart.length}</span>
              </button>
            </div>
          )}
        </>
      )}

      {/* Cart modal */}
      <Modal open={cartOpen} onClose={() => setCartOpen(false)}>
        <ModalHeader title={`Movimentação — ${cart.length} ${cart.length === 1 ? 'item' : 'itens'}`} onClose={() => setCartOpen(false)} />
        <ModalBody className="relative">
          <LoadingOverlay loading={loading} />
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="text-left text-xs font-semibold uppercase text-slate-500 dark:text-zinc-400">
                <tr>
                  <th className="pb-2 pr-4 min-w-[160px]">Material</th>
                  <th className="pb-2 pr-4 w-32">Qtde.*</th>
                  <th className="pb-2 pr-4 w-20">UN.</th>
                  <th className="pb-2 pr-4 w-32">Qtde./Emb.</th>
                  <th className="pb-2 pr-4 w-28">Preço Total</th>
                  <th className="pb-2 w-8"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-zinc-800">
                {cart.map((item, idx) => (
                  <tr key={item.materialStockId}>
                    <td className="py-2 pr-4 text-slate-700 dark:text-zinc-200 text-xs leading-tight">{item.materialName}</td>
                    <td className="py-2 pr-4">
                      <input type="text" inputMode="decimal" value={item.inputQuantity}
                        onChange={(e) => updateCartItem(item.materialStockId, 'inputQuantity', e.target.value.replace(/[^0-9.]/g, ''))}
                        placeholder="Qtde."
                        className="w-full rounded-lg border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-2 py-1 text-sm text-slate-800 dark:text-zinc-100 text-right outline-none focus:border-indigo-400 transition-colors"
                        aria-label={`Quantidade item ${idx + 1}`}
                      />
                    </td>
                    <td className="py-2 pr-4 text-slate-500 dark:text-zinc-400 text-xs">{item.buyUnit}</td>
                    <td className="py-2 pr-4">
                      {item.requestUnit !== item.buyUnit ? (
                        <input type="text" inputMode="decimal" value={item.quantityPackage}
                          onChange={(e) => updateCartItem(item.materialStockId, 'quantityPackage', e.target.value.replace(/[^0-9.]/g, ''))}
                          placeholder="Qtde./emb."
                          className="w-full rounded-lg border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-2 py-1 text-sm text-slate-800 dark:text-zinc-100 text-right outline-none focus:border-indigo-400 transition-colors"
                          aria-label={`Quantidade por embalagem item ${idx + 1}`}
                        />
                      ) : (
                        <span className="text-xs text-slate-400 dark:text-zinc-500">N/A</span>
                      )}
                    </td>
                    <td className="py-2 pr-4">
                      <input type="text" inputMode="decimal" value={item.priceTotal}
                        onChange={(e) => updateCartItem(item.materialStockId, 'priceTotal', e.target.value.replace(/[^0-9.]/g, ''))}
                        placeholder="R$ 0,00"
                        className="w-full rounded-lg border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-2 py-1 text-sm text-slate-800 dark:text-zinc-100 text-right outline-none focus:border-indigo-400 transition-colors"
                        aria-label={`Preço total item ${idx + 1}`}
                      />
                    </td>
                    <td className="py-2">
                      <button type="button"
                        onClick={() => setCart(prev => prev.filter(c => c.materialStockId !== item.materialStockId))}
                        className="flex h-6 w-6 items-center justify-center rounded-lg text-slate-400 hover:bg-red-50 hover:text-red-500 dark:hover:bg-red-900/20 dark:hover:text-red-400 transition-colors">
                        <i className="pi pi-times text-[10px]" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </ModalBody>
        <ModalFooter>
          <button type="button" onClick={() => setCartOpen(false)}
            className="rounded-xl border border-slate-200 dark:border-zinc-700 px-4 py-2 text-sm font-semibold text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors">
            Continuar editando
          </button>
          <button type="button" disabled={loading || cart.length === 0} onClick={() => void handleSubmit()}
            className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500 disabled:opacity-50 transition-colors">
            {loading && <i className="pi pi-spin pi-spinner mr-1.5 text-xs" />}
            Enviar para aprovação
          </button>
        </ModalFooter>
      </Modal>
    </section>
  );
}
