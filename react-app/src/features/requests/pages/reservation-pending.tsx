import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuthStore } from '@/core/auth/useAuthStore';
import { requestApi } from '@/features/requests/api/requests-api';
import { stockApi } from '@/features/stock/api/stock-api';
import { stockistApi } from '@/features/stock/api/stockist-api';
import { GlassListbox } from '@/shared/components/glass-list-box';
import { useNotify } from '@/shared/hooks/use-notify';
import { useAppStore } from '@/store/use-app-store';
import type {
  DepositByStockist,
  LastStockUsage,
  OrderActionPayload,
  OrderDto,
  OrdersByCaseResponse,
  OrdersByCaseView,
  ReplyPayload,
  ReservationStatus,
  StockBalance,
  StockistModel,
} from '../types/reservation';

const STATUS_LABEL: Record<ReservationStatus, string> = {
  PENDING: 'Materiais Pendentes de Aprovação',
  APPROVED: 'Materiais Disponíveis para Coleta',
};

interface ReservationPendingState {
  loading: boolean;
  loadingButton: boolean;
  status: ReservationStatus;
  deposits: DepositByStockist[];
  stockists: StockistModel[];
  orders: OrdersByCaseView[];
  ordersBackup: OrdersByCaseView[];
  depositName: string | null;
  isAdm: boolean;
  currentStock: StockBalance[];
  lastStock: LastStockUsage[];
  replies: ReplyPayload;
  collected: OrderActionPayload[];
  modalSendData: boolean;
}

interface BootstrapParams {
  status: ReservationStatus;
  isAdmin: boolean;
  userUuid: string;
}

interface ActionResult {
  ok: boolean;
  message?: string;
  type?: 'success' | 'warn' | 'error' | 'info';
}

const INITIAL_STATE: ReservationPendingState = {
  loading: false,
  loadingButton: false,
  status: 'PENDING',
  deposits: [],
  stockists: [],
  orders: [],
  ordersBackup: [],
  depositName: null,
  isAdm: false,
  currentStock: [],
  lastStock: [],
  replies: {
    approved: [],
    rejected: [],
  },
  collected: [],
  modalSendData: false,
};

const badgeClassByStatus = (status: OrderDto['internStatus']) => {
  if (status === 'APROVADO' || status === 'COLETADO') {
    return 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300';
  }

  if (status === 'REJEITADO') {
    return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300';
  }

  return 'bg-slate-200 text-slate-700 dark:bg-zinc-800 dark:text-zinc-300';
};

const normalizeFloatInput = (value: string) => {
  const digits = value.replace(/[^0-9.]/g, '');
  const parts = digits.split('.');
  if (parts.length <= 2) return digits;
  return `${parts[0]}.${parts.slice(1).join('')}`;
};

const buildItemKey = (target: OrderActionPayload) =>
  `${target.reserveId ?? 'null'}|${target.order.orderId ?? 'null'}|${target.order.materialId}`;

const isSameTarget = (a: OrderActionPayload, b: OrderActionPayload) => buildItemKey(a) === buildItemKey(b);

const toActionPayload = (order: OrderDto): OrderActionPayload => ({
  reserveId: order.reserveId,
  order: {
    orderId: order.orderId,
    materialId: order.materialId,
    quantity: order.requestQuantity,
  },
});

const toView = (groups: OrdersByCaseResponse[]): OrdersByCaseView[] =>
  groups.map((group) => ({
    ...group,
    reservations: group.orders.map((item) => ({
      ...item,
      uniqueId: item.reserveId ?? item.orderId ?? item.materialId,
    })),
  }));

const withUpdatedOrders = (
  groups: OrdersByCaseView[],
  updater: (order: OrderDto) => OrderDto,
): OrdersByCaseView[] =>
  groups.map((group) => {
    const nextOrders = group.orders.map(updater);
    return {
      ...group,
      orders: nextOrders,
      reservations: nextOrders.map((item) => ({
        ...item,
        uniqueId: item.reserveId ?? item.orderId ?? item.materialId,
      })),
    };
  });

const applyReplyAction = (
  current: ReservationPendingState,
  order: OrderDto,
  action: 'APPROVE' | 'REJECT' | 'COLLECT',
): { nextState: ReservationPendingState; result: ActionResult } => {
  const target = toActionPayload(order);
  let nextState: ReservationPendingState = {
    ...current,
    loadingButton: true,
  };

  const setOrderStatus = (nextStatus: OrderDto['internStatus']) => {
    nextState = {
      ...nextState,
      orders: withUpdatedOrders(nextState.orders, (currentOrder) => {
        const matchesOrder =
          target.order.orderId !== null &&
          currentOrder.orderId === target.order.orderId &&
          currentOrder.materialId === target.order.materialId;

        const matchesReserve =
          target.reserveId !== null &&
          currentOrder.reserveId === target.reserveId;

        return matchesOrder || matchesReserve
          ? {
              ...currentOrder,
              internStatus: nextStatus,
            }
          : currentOrder;
      }),
    };
  };

  const resetQuantity = () => {
    if (target.order.orderId === null) return;

    nextState = {
      ...nextState,
      orders: withUpdatedOrders(nextState.orders, (currentOrder) => {
        if (
          currentOrder.orderId === target.order.orderId &&
          currentOrder.materialId === target.order.materialId
        ) {
          return { ...currentOrder, requestQuantity: null };
        }
        return currentOrder;
      }),
    };
  };

  const debitStockQuantity = (): ActionResult => {
    const quantity = Number(target.order.quantity ?? 0);
    const materialId = target.order.materialId;

    const stockIndex = nextState.currentStock.findIndex((stock) => stock.materialId === materialId);
    if (stockIndex === -1) {
      resetQuantity();
      return {
        ok: false,
        message: 'Não foi possível localizar o estoque do material.',
        type: 'error',
      };
    }

    const nextStock = [...nextState.currentStock];
    const nextLastStock = [...nextState.lastStock];

    let usageIndex = nextLastStock.findIndex(
      (stock) =>
        stock.materialId === materialId &&
        (stock.orderId === target.order.orderId || stock.reserveId === target.reserveId),
    );

    if (usageIndex === -1) {
      nextLastStock.push({
        reserveId: target.reserveId,
        orderId: target.order.orderId,
        materialId,
        quantity: 0,
      });
      usageIndex = nextLastStock.length - 1;
    }

    const currentStockQty = nextStock[stockIndex].stockQuantity;
    const previousQty = nextLastStock[usageIndex].quantity;
    const availableStock = currentStockQty + previousQty;

    if (availableStock < quantity) {
      resetQuantity();
      return {
        ok: false,
        message: `Não há estoque disponível para esse material. Quantidade atual: ${currentStockQty}.`,
        type: 'warn',
      };
    }

    nextStock[stockIndex] = {
      ...nextStock[stockIndex],
      stockQuantity: availableStock - quantity,
    };

    nextLastStock[usageIndex] = {
      ...nextLastStock[usageIndex],
      quantity,
    };

    nextState = {
      ...nextState,
      currentStock: nextStock,
      lastStock: nextLastStock,
    };

    return {
      ok: true,
      message: 'Quantidade atribuída com sucesso.',
      type: 'info',
    };
  };

  const returnStockQuantity = () => {
    const materialId = target.order.materialId;

    const stockIndex = nextState.currentStock.findIndex((stock) => stock.materialId === materialId);
    if (stockIndex === -1) return;

    const usageIndex = nextState.lastStock.findIndex(
      (stock) =>
        stock.materialId === materialId &&
        (stock.orderId === target.order.orderId || stock.reserveId === target.reserveId),
    );

    if (usageIndex === -1) return;

    const nextStock = [...nextState.currentStock];
    const nextLastStock = [...nextState.lastStock];

    nextStock[stockIndex] = {
      ...nextStock[stockIndex],
      stockQuantity: nextStock[stockIndex].stockQuantity + nextLastStock[usageIndex].quantity,
    };

    nextLastStock[usageIndex] = {
      ...nextLastStock[usageIndex],
      quantity: 0,
    };

    nextState = {
      ...nextState,
      currentStock: nextStock,
      lastStock: nextLastStock,
    };
  };

  if (action === 'APPROVE') {
    const quantity = target.order.quantity;
    if (!quantity || quantity === '0') {
      nextState = {
        ...nextState,
        loadingButton: false,
      };
      return {
        nextState,
        result: {
          ok: false,
          message: 'Informe a quantidade a ser liberada.',
          type: 'warn',
        },
      };
    }

    const stockResult = debitStockQuantity();
    if (!stockResult.ok) {
      nextState = {
        ...nextState,
        loadingButton: false,
      };
      return {
        nextState,
        result: stockResult,
      };
    }

    const nextRejected = nextState.replies.rejected.filter((item) => !isSameTarget(item, target));
    const approvedExists = nextState.replies.approved.some((item) => isSameTarget(item, target));

    const nextApproved = approvedExists
      ? nextState.replies.approved
      : [...nextState.replies.approved, target];

    nextState = {
      ...nextState,
      replies: {
        approved: nextApproved,
        rejected: nextRejected,
      },
      loadingButton: false,
    };

    setOrderStatus('APROVADO');

    return {
      nextState,
      result: stockResult,
    };
  }

  if (action === 'REJECT') {
    returnStockQuantity();
    const nextApproved = nextState.replies.approved.filter((item) => !isSameTarget(item, target));
    const rejectedExists = nextState.replies.rejected.some((item) => isSameTarget(item, target));

    const nextRejected = rejectedExists
      ? nextState.replies.rejected
      : [...nextState.replies.rejected, target];

    nextState = {
      ...nextState,
      replies: {
        approved: nextApproved,
        rejected: nextRejected,
      },
      loadingButton: false,
    };

    setOrderStatus('REJEITADO');

    return {
      nextState,
      result: {
        ok: true,
      },
    };
  }

  const stockResult = debitStockQuantity();
  if (!stockResult.ok) {
    nextState = {
      ...nextState,
      loadingButton: false,
    };
    return {
      nextState,
      result: stockResult,
    };
  }

  const nextCollected = [
    ...nextState.collected.filter((item) => !isSameTarget(item, target)),
    target,
  ];

  nextState = {
    ...nextState,
    collected: nextCollected,
    loadingButton: false,
  };

  setOrderStatus('COLETADO');

  return {
    nextState,
    result: stockResult,
  };
};

export default function ReservationPending() {
  const [searchParams] = useSearchParams();
  const userUuid = useAuthStore((state) => state.user?.uuid ?? null);
  const isAdmin = useAuthStore((state) => state.user?.roles.includes('ADMIN') ?? false);
  const setPageContext = useAppStore((state) => state.setPageContext);
  const { notify } = useNotify();

  const [pendingState, setPendingState] = useState<ReservationPendingState>(INITIAL_STATE);

  const {
    loading,
    loadingButton,
    status,
    deposits,
    stockists,
    orders,
    depositName,
    isAdm,
    currentStock,
    replies,
    collected,
    modalSendData,
  } = pendingState;

  const routeStatus = useMemo<ReservationStatus>(() => {
    const value = searchParams.get('status')?.toUpperCase();
    return value === 'APPROVED' ? 'APPROVED' : 'PENDING';
  }, [searchParams]);

  const loadReservations = useCallback(
    async (
      depositId: number,
      nextStatus: ReservationStatus,
      depositsOverride?: DepositByStockist[],
    ) => {
      setPendingState((previous) => ({
        ...previous,
        loading: true,
      }));

      try {
        const groups = await requestApi.getReservation(depositId, nextStatus);
        const view = toView(groups);
        const stockMap = new Map<number, StockBalance>();

        view.forEach((group) => {
          group.orders.forEach((order) => {
            if (!stockMap.has(order.materialId)) {
              stockMap.set(order.materialId, {
                materialId: order.materialId,
                stockQuantity: order.stockQuantity,
              });
            }
          });
        });

        setPendingState((previous) => {
          const activeDeposits = depositsOverride ?? previous.deposits;

          return {
            ...previous,
            ...(depositsOverride ? { deposits: depositsOverride } : {}),
            status: nextStatus,
            depositName:
              activeDeposits.find((deposit) => deposit.depositId === depositId)?.depositName ?? null,
            orders: view,
            ordersBackup: view,
            replies: {
              approved: [],
              rejected: [],
            },
            collected: [],
            currentStock: Array.from(stockMap.values()),
            lastStock: [],
          };
        });
      } finally {
        setPendingState((previous) => ({
          ...previous,
          loading: false,
        }));
      }
    },
    [],
  );

  const bootstrap = useCallback(
    async ({ status: bootstrapStatus, isAdmin: bootstrapAdmin, userUuid: bootstrapUserUuid }: BootstrapParams) => {
      setPendingState((previous) => ({
        ...previous,
        loading: true,
        status: bootstrapStatus,
        deposits: [],
        stockists: [],
        orders: [],
        ordersBackup: [],
        depositName: null,
        isAdm: bootstrapAdmin,
        currentStock: [],
        lastStock: [],
        replies: {
          approved: [],
          rejected: [],
        },
        collected: [],
        modalSendData: false,
      }));

      try {
        if (bootstrapAdmin) {
          const [depositsData, stockistsData] = await Promise.all([
            stockApi.getDeposits(),
            stockistApi.getStockists(),
          ]);

          const nextDeposits = depositsData
            .filter((deposit) => !deposit.isTruck)
            .map<DepositByStockist>((deposit) => ({
              depositId: deposit.idDeposit,
              depositName: deposit.depositName,
              depositAddress: deposit.depositAddress,
              depositPhone: deposit.depositPhone,
            }));

          setPendingState((previous) => ({
            ...previous,
            deposits: nextDeposits,
            stockists: stockistsData,
          }));
          return;
        }

        const nextDeposits = await stockApi.getDepositsByStockist(bootstrapUserUuid);

        if (nextDeposits.length === 1) {
          await loadReservations(nextDeposits[0].depositId, bootstrapStatus, nextDeposits);
          return;
        }

        setPendingState((previous) => ({
          ...previous,
          deposits: nextDeposits,
        }));
      } finally {
        setPendingState((previous) => ({
          ...previous,
          loading: false,
        }));
      }
    },
    [loadReservations],
  );

  useEffect(() => {
    setPageContext(['Requisições', STATUS_LABEL[routeStatus]], STATUS_LABEL[routeStatus]);

    if (!userUuid) return;

    bootstrap({
      status: routeStatus,
      isAdmin,
      userUuid,
    }).catch((error: unknown) => {
      const message = error instanceof Error ? error.message : 'Erro ao carregar requisições.';
      notify(message, 'error');
    });
  }, [bootstrap, isAdmin, notify, routeStatus, setPageContext, userUuid]);

  const stockByMaterial = useMemo(() => {
    const map = new Map<number, number>();
    currentStock.forEach((stock) => map.set(stock.materialId, stock.stockQuantity));
    return map;
  }, [currentStock]);

  const setOrderQuantity = (order: OrderDto, quantity: string) => {
    setPendingState((previous) => ({
      ...previous,
      orders: withUpdatedOrders(previous.orders, (currentOrder) => {
        if (currentOrder.orderId === order.orderId && currentOrder.materialId === order.materialId) {
          return { ...currentOrder, requestQuantity: quantity };
        }
        return currentOrder;
      }),
    }));
  };

  const filterOrders = (value: string) => {
    const term = value.trim().toLowerCase();

    setPendingState((previous) => ({
      ...previous,
      orders: !term
        ? previous.ordersBackup
        : previous.ordersBackup.filter((group) => group.description.toLowerCase().includes(term)),
    }));
  };

  const onReply = (order: OrderDto, action: 'APPROVE' | 'REJECT' | 'COLLECT') => {
    let result: ActionResult = { ok: true };

    setPendingState((previous) => {
      const applied = applyReplyAction(previous, order, action);
      result = applied.result;
      return applied.nextState;
    });

    if (result.message && result.type) {
      notify(result.message, result.type);
    }
  };

  const onSend = () => {
    const hasAtLeastOneResponse = orders.some((group) =>
      group.orders.some((order) => order.internStatus !== null && order.internStatus !== undefined),
    );

    if (!hasAtLeastOneResponse) {
      notify('Nenhuma reserva foi respondida.', 'warn');
      return;
    }

    for (const group of orders) {
      const allFilled = group.orders.every(
        (order) => order.internStatus !== null && order.internStatus !== undefined,
      );
      const noneFilled = group.orders.every(
        (order) => order.internStatus === null || order.internStatus === undefined,
      );
      const hasApprovedWithoutQuantity = group.orders.some(
        (order) => (!order.requestQuantity || order.requestQuantity === '0') && order.internStatus === 'APROVADO',
      );

      if (!allFilled && !noneFilled) {
        notify(`Responda todas as reservas pendentes da ${group.description}.`, 'warn');
        return;
      }

      if (hasApprovedWithoutQuantity) {
        notify(`Informe a quantidade aprovada dos materiais para ${group.description}.`, 'warn');
        return;
      }
    }

    setPendingState((previous) => ({
      ...previous,
      modalSendData: true,
    }));
  };

  const onConfirmSend = async () => {
    const snapshot = {
      status,
      replies,
      collected,
      orders,
    };

    setPendingState((previous) => ({
      ...previous,
      loading: true,
    }));

    try {
      const targets =
        snapshot.status === 'PENDING'
          ? [...snapshot.replies.approved, ...snapshot.replies.rejected]
          : snapshot.collected;

      if (snapshot.status === 'PENDING') {
        await requestApi.reply(snapshot.replies);
      } else {
        await requestApi.markAsCollected(snapshot.collected);
      }

      const targetKeys = new Set(targets.map((target) => buildItemKey(target)));

      const filtered = snapshot.orders
        .map((group) => {
          const nextOrders = group.orders.filter(
            (order) => !targetKeys.has(buildItemKey(toActionPayload(order))),
          );

          return {
            ...group,
            orders: nextOrders,
            reservations: nextOrders.map((item) => ({
              ...item,
              uniqueId: item.reserveId ?? item.orderId ?? item.materialId,
            })),
          };
        })
        .filter((group) => group.orders.length > 0);

      setPendingState((previous) => ({
        ...previous,
        orders: filtered,
        ordersBackup: filtered,
        replies: {
          approved: [],
          rejected: [],
        },
        collected: [],
        modalSendData: false,
      }));

      if (snapshot.status === 'PENDING') {
        notify(
          'Materiais aprovados foram encaminhados. Use a fila de coleta para concluir a retirada e atualizar estoque.',
          'success',
        );
      } else {
        notify('A equipe já pode iniciar a execução com esses materiais.', 'success');
      }
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Não foi possível concluir o envio.';
      notify(message, 'error');
    } finally {
      setPendingState((previous) => ({
        ...previous,
        loading: false,
      }));
    }
  };

  const hasAdminMissingSetup = isAdm && (deposits.length === 0 || stockists.length === 0);

  return (
    <section className="p-4 md:p-6 space-y-4">
      <header className="rounded-2xl border border-slate-200 bg-white px-4 py-4 dark:border-zinc-800 dark:bg-zinc-900">
        <div className="flex flex-wrap items-center gap-3 justify-between">
          <div>
            <h1 className="text-lg font-semibold text-slate-800 dark:text-zinc-100">
              {STATUS_LABEL[status]}
            </h1>
            <p className="text-sm text-slate-500 dark:text-zinc-400">
              {depositName ? `Almoxarifado: ${depositName}` : 'Selecione um almoxarifado para iniciar.'}
            </p>
          </div>

          {depositName && (
            <div className="relative max-w-xs w-full">
              <i className="pi pi-search absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                placeholder="Pesquisar requisição"
                onChange={(event) => filterOrders(event.target.value)}
                className="w-full rounded-xl border border-slate-200 bg-white py-2.5 pl-10 pr-3 text-sm text-slate-800 outline-none transition focus:border-blue-400 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
              />
            </div>
          )}
        </div>
      </header>

      {loading && (
        <div className="flex min-h-64 items-center justify-center rounded-2xl border border-slate-200 bg-white dark:border-zinc-800 dark:bg-zinc-900">
          <i className="pi pi-spin pi-spinner text-2xl text-blue-500" />
        </div>
      )}

      {!loading && hasAdminMissingSetup && (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-6 dark:border-amber-900/40 dark:bg-amber-950/20">
          <h2 className="text-base font-semibold text-amber-800 dark:text-amber-200">Antes de exibir requisições</h2>
          <ul className="mt-3 list-disc space-y-1 pl-5 text-sm text-amber-700 dark:text-amber-300">
            {deposits.length === 0 && <li>Cadastre ao menos um almoxarifado.</li>}
            {stockists.length === 0 && <li>Cadastre ao menos um estoquista.</li>}
          </ul>
        </div>
      )}

      {!loading && !hasAdminMissingSetup && !depositName && deposits.length > 0 && (
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-900">
          <label className="mb-2 block text-sm font-medium text-slate-700 dark:text-zinc-200">
            Selecione o almoxarifado
          </label>
          <GlassListbox
            value={null}
            onChange={(value) => {
              if (value === null) return;
              loadReservations(Number(value), status).catch((error: unknown) => {
                const message = error instanceof Error ? error.message : 'Erro ao buscar reservas.';
                notify(message, 'error');
              });
            }}
            placeholder="Escolha um almoxarifado"
            options={[
              { value: null, label: 'Escolha um almoxarifado' },
              ...deposits.map((deposit) => ({
                value: deposit.depositId,
                label: deposit.depositName,
              })),
            ]}
          />
        </div>
      )}

      {!loading && !hasAdminMissingSetup && deposits.length === 0 && (
        <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center dark:border-zinc-800 dark:bg-zinc-900">
          <i className="pi pi-exclamation-triangle text-4xl text-amber-500" />
          <p className="mt-3 text-sm text-slate-600 dark:text-zinc-300">
            Você não é estoquista de nenhum almoxarifado no sistema.
          </p>
        </div>
      )}

      {!loading && depositName && orders.length === 0 && (
        <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center dark:border-zinc-800 dark:bg-zinc-900">
          <i className="pi pi-inbox text-4xl text-slate-400" />
          <p className="mt-3 text-sm text-slate-600 dark:text-zinc-300">Nenhuma pendência encontrada.</p>
        </div>
      )}

      {!loading && orders.length > 0 && (
        <>
          <div className="space-y-6">
            {orders.map((group) => (
              <article key={group.description} className="rounded-2xl border border-slate-200 bg-white dark:border-zinc-800 dark:bg-zinc-900">
                <header className="flex items-center justify-between gap-3 border-b border-slate-100 px-4 py-3 dark:border-zinc-800">
                  <h2 className="text-sm font-semibold tracking-wide text-slate-800 dark:text-zinc-100">
                    {group.description}
                  </h2>
                  <span className="rounded-full bg-blue-100 px-2.5 py-1 text-xs font-semibold text-blue-700 dark:bg-blue-900/30 dark:text-blue-300">
                    {group.teamName ?? 'Equipe não identificada'}
                  </span>
                </header>

                <div className="overflow-x-auto">
                  <table className="min-w-[900px] w-full">
                    <thead className="bg-slate-50 text-left text-xs uppercase tracking-wider text-slate-500 dark:bg-zinc-900/50 dark:text-zinc-400">
                      <tr>
                        <th className="px-4 py-3 font-semibold">Material</th>
                        <th className="px-4 py-3 font-semibold">Qtde. Solicitada</th>
                        <th className="px-4 py-3 font-semibold">Qtde. em Estoque</th>
                        <th className="px-4 py-3 font-semibold">Status</th>
                        <th className="px-4 py-3 font-semibold text-right">Ações</th>
                      </tr>
                    </thead>

                    <tbody>
                      {group.orders.map((order) => {
                        const stock = stockByMaterial.get(order.materialId) ?? 0;

                        return (
                          <tr key={`${order.reserveId ?? 'n'}-${order.orderId ?? 'n'}-${order.materialId}`} className="border-t border-slate-100 dark:border-zinc-800">
                            <td className="px-4 py-3 text-sm text-slate-700 dark:text-zinc-200">{order.materialName}</td>

                            <td className="px-4 py-3">
                              {order.orderId !== null && status === 'PENDING' ? (
                                <input
                                  value={order.requestQuantity ?? ''}
                                  onChange={(event) =>
                                    setOrderQuantity(order, normalizeFloatInput(event.target.value))
                                  }
                                  placeholder="Informe a quantidade"
                                  className="w-40 rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-sm text-slate-800 outline-none transition focus:border-blue-400 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
                                />
                              ) : (
                                <span className="text-sm text-slate-700 dark:text-zinc-200">{order.requestQuantity ?? 'A definir'}</span>
                              )}
                            </td>

                            <td className="px-4 py-3 text-sm text-slate-700 dark:text-zinc-200">{stock}</td>

                            <td className="px-4 py-3">
                              <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${badgeClassByStatus(order.internStatus)}`}>
                                {order.internStatus ?? 'PENDENTE'}
                              </span>
                            </td>

                            <td className="px-4 py-3">
                              <div className="flex items-center justify-end gap-2">
                                {status === 'PENDING' ? (
                                  <>
                                    <button
                                      type="button"
                                      onClick={() => onReply(order, 'REJECT')}
                                      disabled={loadingButton}
                                      className="rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-100 disabled:opacity-40 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-800"
                                    >
                                      Rejeitar
                                    </button>
                                    <button
                                      type="button"
                                      onClick={() => onReply(order, 'APPROVE')}
                                      disabled={loadingButton}
                                      className="rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-blue-500 disabled:opacity-40"
                                    >
                                      Aprovar
                                    </button>
                                  </>
                                ) : (
                                  <button
                                    type="button"
                                    onClick={() => onReply(order, 'COLLECT')}
                                    disabled={loadingButton}
                                    className="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-emerald-500 disabled:opacity-40"
                                  >
                                    Confirmar coleta
                                  </button>
                                )}
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </article>
            ))}
          </div>

          <div className="sticky bottom-3 z-10 flex justify-end">
            <button
              type="button"
              onClick={onSend}
              disabled={loading}
              className="rounded-xl bg-slate-900 px-5 py-2.5 text-sm font-semibold text-white shadow-lg transition hover:bg-slate-700 disabled:opacity-40 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-300"
            >
              Finalizar
            </button>
          </div>
        </>
      )}

      {modalSendData && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 px-4">
          <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-5 shadow-xl dark:border-zinc-800 dark:bg-zinc-900">
            <h3 className="text-base font-semibold text-slate-800 dark:text-zinc-100">Confirma o envio dos dados?</h3>
            <p className="mt-2 text-sm text-slate-600 dark:text-zinc-300">
              Após confirmar, as respostas selecionadas serão enviadas para processamento.
            </p>

            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() =>
                  setPendingState((previous) => ({
                    ...previous,
                    modalSendData: false,
                  }))
                }
                className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-800"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={() => void onConfirmSend()}
                className="rounded-lg bg-blue-600 px-3 py-1.5 text-sm font-semibold text-white transition hover:bg-blue-500"
              >
                Confirmar
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
